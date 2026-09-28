import { NextResponse } from "next/server";
import pool from "@/lib/db";

export async function GET() {
  const client = await pool.connect();
  try {
    const { rows } = await client.query(
      `SELECT user_id, full_name, email FROM users WHERE account_status = 'ACTIVE' ORDER BY user_id ASC`
    );
    return NextResponse.json(rows.map((r) => ({ userId: Number(r.user_id), fullName: r.full_name, email: r.email })));
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: msg }, { status: 500 });
  } finally {
    client.release();
  }
}
