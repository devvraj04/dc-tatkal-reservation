import { NextResponse } from "next/server";
import pool from "@/lib/db";

export async function GET() {
  const client = await pool.connect();
  try {
    const { rows } = await client.query(
      `SELECT ts.schedule_id, ts.train_no, t.train_name, 
              ts.journey_date, ts.departure_datetime, ts.arrival_datetime,
              t.source_station_code, t.destination_station_code
       FROM train_schedules ts
       JOIN trains t ON ts.train_no = t.train_no
       WHERE ts.schedule_status = 'SCHEDULED'
       ORDER BY ts.journey_date ASC, t.train_name ASC`
    );

    return NextResponse.json(
      rows.map((r) => ({
        scheduleId: Number(r.schedule_id),
        trainNo: Number(r.train_no),
        trainName: r.train_name,
        journeyDate: r.journey_date,
        departureTime: new Date(r.departure_datetime).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }),
        arrivalTime: new Date(r.arrival_datetime).toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" }),
        sourceStation: r.source_station_code,
        destinationStation: r.destination_station_code,
      }))
    );
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: msg }, { status: 500 });
  } finally {
    client.release();
  }
}
