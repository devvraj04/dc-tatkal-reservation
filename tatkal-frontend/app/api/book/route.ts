import { NextRequest, NextResponse } from "next/server";
import pool from "@/lib/db";

interface PassengerInput {
  passengerId?: number;
  name?: string;
  age?: number;
  gender?: string;
  berthPreference?: string;
  idProofType?: string;
  idProofNumber?: string;
}

const FARE_MAP: Record<string, number> = {
  "1A": 1800, "2A": 1100, "3A": 650, "SL": 250,
};

function generatePnr(): string {
  return String(1000000000 + Math.floor(Math.random() * 900000000));
}

export async function POST(req: NextRequest) {
  const body = await req.json();
  const { userId, scheduleId, coachType, passengers, paymentMode } = body as {
    userId: number;
    scheduleId: number;
    coachType: string;
    passengers: PassengerInput[];
    paymentMode: string;
  };

  if (!passengers || passengers.length === 0) {
    return NextResponse.json({ error: "No passengers specified." }, { status: 400 });
  }

  const client = await pool.connect();
  try {
    await client.query("BEGIN");

    // 1. Validate schedule
    const { rows: schedRows } = await client.query(
      `SELECT t.source_station_code, t.destination_station_code
       FROM train_schedules ts JOIN trains t ON ts.train_no = t.train_no
       WHERE ts.schedule_id = $1`,
      [scheduleId]
    );
    if (schedRows.length === 0) {
      await client.query("ROLLBACK");
      return NextResponse.json({ error: "Invalid train schedule ID." }, { status: 404 });
    }
    const { source_station_code: srcStation, destination_station_code: destStation } = schedRows[0];

    // 2. Lock available seats
    const { rows: seatRows } = await client.query(
      `SELECT sa.allocation_id, sa.seat_id, s.seat_number, c.coach_number, s.berth_type
       FROM seat_allocations sa
       JOIN seats s ON sa.seat_id = s.seat_id
       JOIN coaches c ON s.coach_id = c.coach_id
       WHERE sa.schedule_id = $1
         AND c.coach_type = $2::coach_type_enum
         AND sa.booking_status = 'AVAILABLE'
       ORDER BY sa.allocation_id
       LIMIT $3
       FOR UPDATE OF sa SKIP LOCKED`,
      [scheduleId, coachType.toUpperCase(), passengers.length]
    );

    const farePerSeat = FARE_MAP[coachType.toUpperCase()] ?? 200;
    const totalFare = farePerSeat * passengers.length;
    const pnr = generatePnr();
    const allConfirmed = seatRows.length === passengers.length;
    const bookingStatus = allConfirmed ? "CONFIRMED" : seatRows.length === 0 ? "WAITING" : "PARTIAL";

    // 3. Resolve passenger IDs
    const resolvedPassengers: { id: number; name: string }[] = [];
    for (const p of passengers) {
      if (p.passengerId && p.passengerId !== -1) {
        const { rows } = await client.query(
          `SELECT passenger_name FROM passengers WHERE passenger_id = $1`, [p.passengerId]
        );
        resolvedPassengers.push({ id: p.passengerId, name: rows[0]?.passenger_name ?? `Passenger #${p.passengerId}` });
      } else {
        const berth = (p.berthPreference && p.berthPreference.trim() !== "") ? p.berthPreference.toUpperCase().trim() : "NO_PREFERENCE";
        const { rows } = await client.query(
          `INSERT INTO passengers (user_id, passenger_name, age, gender, berth_preference, id_proof_type, id_proof_number)
           VALUES ($1, $2, $3, $4::gender_enum, $5::berth_preference_enum, $6, $7)
           RETURNING passenger_id`,
          [userId, p.name, p.age, (p.gender ?? "MALE").toUpperCase(), berth, p.idProofType, p.idProofNumber]
        );
        resolvedPassengers.push({ id: rows[0].passenger_id, name: p.name! });
      }
    }

    // 4. Insert booking
    await client.query(
      `INSERT INTO bookings (pnr, user_id, schedule_id, source_station_code, destination_station_code,
         coach_type, booking_type, quota, booking_status, total_fare)
       VALUES ($1,$2,$3,$4,$5,$6::coach_type_enum,'TATKAL','TATKAL',$7::booking_status_enum,$8)`,
      [pnr, userId, scheduleId, srcStation, destStation, coachType.toUpperCase(), bookingStatus, totalFare]
    );

    // 5. Assign seats / waitlist
    const seatAssignments = [];
    for (let i = 0; i < resolvedPassengers.length; i++) {
      const { id: passId, name: passName } = resolvedPassengers[i];
      if (i < seatRows.length) {
        const seat = seatRows[i];
        await client.query(
          `UPDATE seat_allocations SET booking_status='BOOKED', booking_type='TATKAL', pnr=$1, allocated_at=CURRENT_TIMESTAMP WHERE allocation_id=$2`,
          [pnr, seat.allocation_id]
        );
        await client.query(
          `INSERT INTO booking_passengers (pnr, passenger_id, allocation_id, passenger_status, fare) VALUES ($1,$2,$3,'CONFIRMED',$4)`,
          [pnr, passId, seat.allocation_id, farePerSeat]
        );
        seatAssignments.push({ passengerName: passName, coachNumber: seat.coach_number, seatNumber: seat.seat_number, berthType: seat.berth_type, status: "CONFIRMED" });
      } else {
        await client.query(
          `INSERT INTO booking_passengers (pnr, passenger_id, allocation_id, passenger_status, fare) VALUES ($1,$2,NULL,'WAITING',$3)`,
          [pnr, passId, farePerSeat]
        );
        const { rows: wlRows } = await client.query(
          `SELECT COALESCE(MAX(waitlist_number),0) AS max_wl FROM waitlists WHERE schedule_id=$1 AND coach_type=$2::coach_type_enum`,
          [scheduleId, coachType.toUpperCase()]
        );
        const nextWl = Number(wlRows[0].max_wl) + 1;
        await client.query(
          `INSERT INTO waitlists (pnr, schedule_id, coach_type, waitlist_number, current_position, waitlist_type, status)
           VALUES ($1,$2,$3::coach_type_enum,$4,$4,'TQWL','WAITING')`,
          [pnr, scheduleId, coachType.toUpperCase(), nextWl]
        );
        seatAssignments.push({ passengerName: passName, coachNumber: null, seatNumber: 0, berthType: null, status: "WAITING" });
      }
    }

    // 6. Payment
    const txnId = `TXN${Date.now()}${Math.floor(Math.random() * 100)}`;
    await client.query(
      `INSERT INTO payments (pnr, amount, payment_mode, transaction_id, payment_status) VALUES ($1,$2,$3::payment_mode_enum,$4,'SUCCESS')`,
      [pnr, totalFare, paymentMode.toUpperCase(), txnId]
    );

    // 7. Notification
    await client.query(
      `INSERT INTO notifications (user_id, pnr, notification_type, message) VALUES ($1,$2,'BOOKING',$3)`,
      [userId, pnr, `Ticket booked successfully for ${passengers.length} passenger(s). PNR: ${pnr}`]
    );

    await client.query("COMMIT");

    return NextResponse.json({
      success: true,
      pnr,
      status: bookingStatus,
      totalFare,
      message: allConfirmed ? "All passengers confirmed." : "Some/all passengers waitlisted.",
      seatAssignments,
    });
  } catch (err: unknown) {
    await client.query("ROLLBACK");
    const msg = err instanceof Error ? err.message : "Unknown error";
    console.error("Booking error:", err);
    return NextResponse.json({ error: "Booking failed: " + msg }, { status: 500 });
  } finally {
    client.release();
  }
}
