import { NextResponse } from "next/server";
import pool from "@/lib/db";

export async function GET() {
  const client = await pool.connect();
  try {
    const { rows } = await client.query(
      `SELECT user_id, full_name, email FROM users WHERE is_leader = TRUE AND account_status = 'ACTIVE'`
    );
    if (rows.length === 0) return NextResponse.json(null);
    return NextResponse.json({ userId: rows[0].user_id, fullName: rows[0].full_name, email: rows[0].email });
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: msg }, { status: 500 });
  } finally {
    client.release();
  }
}
