import { NextRequest, NextResponse } from "next/server";
import pool from "@/lib/db";

export async function GET(req: NextRequest) {
  const { searchParams } = new URL(req.url);
  const source = searchParams.get("source")?.toUpperCase();
  const destination = searchParams.get("destination")?.toUpperCase();
  const date = searchParams.get("date");

  if (!source || !destination || !date) {
    return NextResponse.json({ error: "source, destination, and date are required." }, { status: 400 });
  }

  const client = await pool.connect();
  try {
    const { rows } = await client.query(
      `SELECT ts.schedule_id, t.train_no, t.train_name,
              r1.departure_time, r2.arrival_time,
              (r2.day_number - r1.day_number) AS day_diff
       FROM train_schedules ts
       JOIN trains t ON ts.train_no = t.train_no
       JOIN routes r1 ON (t.train_no = r1.train_no AND r1.station_code = $1)
       JOIN routes r2 ON (t.train_no = r2.train_no AND r2.station_code = $2)
       WHERE r1.stop_number < r2.stop_number
         AND ts.journey_date = $3::DATE
         AND ts.schedule_status = 'SCHEDULED'
       ORDER BY r1.departure_time ASC`,
      [source, destination, date]
    );

    const results = rows.map((r) => ({
      scheduleId: Number(r.schedule_id),
      trainNo: Number(r.train_no),
      trainName: r.train_name,
      departureTime: r.departure_time,
      arrivalTime: r.arrival_time,
      dayDiff: Number(r.day_diff),
    }));

    return NextResponse.json(results);
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: "Database error: " + msg }, { status: 500 });
  } finally {
    client.release();
  }
}
