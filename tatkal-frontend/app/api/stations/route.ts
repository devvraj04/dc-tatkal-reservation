import { NextResponse } from "next/server";
import pool from "@/lib/db";

export async function GET() {
  const client = await pool.connect();
  try {
    const { rows } = await client.query(
      `SELECT station_code, station_name, city, state, railway_zone 
       FROM stations 
       ORDER BY city ASC, station_name ASC`
    );
    return NextResponse.json(
      rows.map((r) => ({
        code: r.station_code,
        name: r.station_name,
        city: r.city,
        state: r.state,
        zone: r.railway_zone,
      }))
    );
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: msg }, { status: 500 });
  } finally {
    client.release();
  }
}
