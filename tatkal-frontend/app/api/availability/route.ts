import { NextRequest, NextResponse } from "next/server";
import pool from "@/lib/db";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const trainNo = searchParams.get("trainNo");
  const date = searchParams.get("date");

  if (!trainNo || !date) {
    return NextResponse.json({ error: "trainNo and date are required." }, { status: 400 });
  }

  const client = await pool.connect();
  try {
    const { rows } = await client.query(
      `SELECT c.coach_type,
              COUNT(CASE WHEN sa.booking_status = 'AVAILABLE' THEN 1 END)::int AS available_seats,
              COUNT(sa.allocation_id)::int AS total_seats
       FROM train_schedules ts
       JOIN coaches c ON ts.train_no = c.train_no
       JOIN seats s ON c.coach_id = s.coach_id
       LEFT JOIN seat_allocations sa ON (ts.schedule_id = sa.schedule_id AND s.seat_id = sa.seat_id)
       WHERE ts.train_no = $1 AND ts.journey_date = $2::DATE
       GROUP BY c.coach_type
       ORDER BY c.coach_type DESC`,
      [Number(trainNo), date]
    );

    const results = rows.map((r) => ({
      coachType: r.coach_type,
      availableSeats: r.available_seats,
      totalSeats: r.total_seats,
    }));

    return NextResponse.json(results);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: "Database error: " + msg }, { status: 500 });
  } finally {
    client.release();
  }
}
