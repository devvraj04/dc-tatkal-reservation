import { NextRequest, NextResponse } from "next/server";
import pool from "@/lib/db";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const userId = searchParams.get("userId");
  if (!userId) {
    return NextResponse.json({ error: "userId is required." }, { status: 400 });
  }

  const client = await pool.connect();
  try {
    const { rows: bookings } = await client.query(
      `SELECT b.pnr, b.source_station_code, b.destination_station_code,
              b.booking_status, b.total_fare, b.booking_timestamp,
              ts.train_no, t.train_name, ts.journey_date
       FROM bookings b
       JOIN train_schedules ts ON b.schedule_id = ts.schedule_id
       JOIN trains t ON ts.train_no = t.train_no
       WHERE b.user_id = $1
       ORDER BY b.booking_timestamp DESC`,
      [userId]
    );

    const result = [];
    for (const b of bookings) {
      const { rows: passengers } = await client.query(
        `SELECT bp.passenger_status, bp.fare, p.passenger_name,
                s.seat_number, c.coach_number, s.berth_type
         FROM booking_passengers bp
         JOIN passengers p ON bp.passenger_id = p.passenger_id
         LEFT JOIN seat_allocations sa ON bp.allocation_id = sa.allocation_id
         LEFT JOIN seats s ON sa.seat_id = s.seat_id
         LEFT JOIN coaches c ON s.coach_id = c.coach_id
         WHERE bp.pnr = $1`,
        [b.pnr]
      );

      result.push({
        pnr: b.pnr,
        trainNo: Number(b.train_no),
        trainName: b.train_name,
        journeyDate: b.journey_date,
        sourceStation: b.source_station_code,
        destinationStation: b.destination_station_code,
        bookingStatus: b.booking_status,
        totalFare: Number(b.total_fare),
        bookingTimestamp: b.booking_timestamp,
        passengers: passengers.map((p) => ({
          passengerName: p.passenger_name,
          coachNumber: p.coach_number ?? null,
          seatNumber: p.seat_number ?? 0,
          berthType: p.berth_type ?? null,
          status: p.passenger_status,
        })),
      });
    }

    return NextResponse.json(result);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: "Database error: " + msg }, { status: 500 });
  } finally {
    client.release();
  }
}
