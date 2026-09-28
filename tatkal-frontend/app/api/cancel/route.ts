import { NextRequest, NextResponse } from "next/server";
import pool from "@/lib/db";

export async function POST(req: NextRequest) {
  const { pnr, userId } = await req.json();
  if (!pnr || !userId) {
    return NextResponse.json({ error: "PNR and userId are required." }, { status: 400 });
  }

  const client = await pool.connect();
  try {
    await client.query("BEGIN");

    // 1. Fetch & lock booking
    const { rows: bookingRows } = await client.query(
      `SELECT user_id, schedule_id, coach_type, booking_status, total_fare FROM bookings WHERE pnr = $1 FOR UPDATE`,
      [pnr]
    );
    if (bookingRows.length === 0) {
      await client.query("ROLLBACK");
      return NextResponse.json({ error: "PNR not found." }, { status: 404 });
    }
    const booking = bookingRows[0];
    if (Number(booking.user_id) !== Number(userId)) {
      await client.query("ROLLBACK");
      return NextResponse.json({ error: "Access denied: PNR does not belong to this user." }, { status: 403 });
    }
    if (booking.booking_status === "CANCELLED") {
      await client.query("ROLLBACK");
      return NextResponse.json({ error: "Ticket is already cancelled." }, { status: 400 });
    }

    // 2. Calculate refund
    const { rows: countRows } = await client.query(
      `SELECT COUNT(*) AS cnt FROM booking_passengers WHERE pnr = $1`, [pnr]
    );
    const passengerCount = Number(countRows[0].cnt);
    const cancelCharge = 100 * passengerCount;
    const totalFare = Number(booking.total_fare);
    const refundAmt = Math.max(0, totalFare - cancelCharge);

    // 3. Cancel booking
    await client.query(`UPDATE bookings SET booking_status='CANCELLED', cancellation_time=CURRENT_TIMESTAMP WHERE pnr=$1`, [pnr]);
    await client.query(`UPDATE waitlists SET status='CANCELLED' WHERE pnr=$1 AND status='WAITING'`, [pnr]);

    // 4. Fetch seat allocations
    const { rows: seatRows } = await client.query(
      `SELECT bp.booking_passenger_id, sa.seat_id, bp.allocation_id, bp.passenger_status
       FROM booking_passengers bp
       LEFT JOIN seat_allocations sa ON bp.allocation_id = sa.allocation_id
       WHERE bp.pnr = $1`,
      [pnr]
    );

    await client.query(`UPDATE booking_passengers SET passenger_status='CANCELLED' WHERE pnr=$1`, [pnr]);

    // 5. Free seats / promote waitlist
    for (const psi of seatRows) {
      if (psi.passenger_status === "CONFIRMED" && psi.allocation_id) {
        const { rows: wlRows } = await client.query(
          `SELECT w.waitlist_id, w.pnr, bp.booking_passenger_id, b.user_id
           FROM waitlists w
           JOIN bookings b ON w.pnr = b.pnr
           JOIN booking_passengers bp ON (w.pnr = bp.pnr AND bp.passenger_status = 'WAITING')
           WHERE w.schedule_id = $1 AND w.coach_type = $2::coach_type_enum AND w.status = 'WAITING'
           ORDER BY w.waitlist_number ASC LIMIT 1 FOR UPDATE`,
          [booking.schedule_id, booking.coach_type]
        );

        if (wlRows.length > 0) {
          const promoted = wlRows[0];
          await client.query(`UPDATE seat_allocations SET pnr=$1, allocated_at=CURRENT_TIMESTAMP WHERE allocation_id=$2`, [promoted.pnr, psi.allocation_id]);
          await client.query(`UPDATE booking_passengers SET allocation_id=$1, passenger_status='CONFIRMED' WHERE booking_passenger_id=$2`, [psi.allocation_id, promoted.booking_passenger_id]);
          await client.query(`UPDATE waitlists SET status='CONFIRMED', confirmed_at=CURRENT_TIMESTAMP WHERE waitlist_id=$1`, [promoted.waitlist_id]);

          const { rows: stillWaiting } = await client.query(`SELECT COUNT(*) AS cnt FROM booking_passengers WHERE pnr=$1 AND passenger_status='WAITING'`, [promoted.pnr]);
          const newStatus = Number(stillWaiting[0].cnt) === 0 ? "CONFIRMED" : "PARTIAL";
          await client.query(`UPDATE bookings SET booking_status=$1 WHERE pnr=$2`, [newStatus, promoted.pnr]);

          await client.query(
            `INSERT INTO notifications (user_id, pnr, notification_type, message) VALUES ($1,$2,'WAITLIST',$3)`,
            [promoted.user_id, promoted.pnr, `Congratulations! Your waitlist ticket under PNR ${promoted.pnr} has been CONFIRMED.`]
          );
        } else {
          await client.query(`UPDATE seat_allocations SET booking_status='AVAILABLE', pnr=NULL, allocated_at=NULL WHERE allocation_id=$1`, [psi.allocation_id]);
        }
      }
    }

    // 6. Cancellation record
    await client.query(
      `INSERT INTO cancellations (pnr, cancelled_by, refund_amount, cancellation_charge) VALUES ($1,$2,$3,$4)`,
      [pnr, userId, refundAmt, cancelCharge]
    );

    // 7. Notification
    await client.query(
      `INSERT INTO notifications (user_id, pnr, notification_type, message) VALUES ($1,$2,'CANCELLATION',$3)`,
      [userId, pnr, `Ticket successfully cancelled for PNR ${pnr}. Refund amount: Rs. ${refundAmt}`]
    );

    await client.query("COMMIT");
    return NextResponse.json({ success: true, pnr, refundAmount: refundAmt, cancellationCharge: cancelCharge, message: "Ticket cancelled successfully." });
  } catch (err: unknown) {
    await client.query("ROLLBACK");
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: "Cancellation failed: " + msg }, { status: 500 });
  } finally {
    client.release();
  }
}
