import { NextRequest, NextResponse } from "next/server";
import pool from "@/lib/db";

export async function POST(req: NextRequest) {
  const { winnerUserId } = await req.json();
  if (!winnerUserId) {
    return NextResponse.json({ error: "winnerUserId is required." }, { status: 400 });
  }

  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    await client.query("UPDATE users SET is_leader = FALSE");
    const { rowCount } = await client.query("UPDATE users SET is_leader = TRUE WHERE user_id = $1", [winnerUserId]);

    if (!rowCount || rowCount === 0) {
      await client.query("ROLLBACK");
      return NextResponse.json({ error: "User not found." }, { status: 404 });
    }

    const { rows } = await client.query("SELECT COUNT(*) AS cnt FROM users WHERE is_leader = TRUE");
    if (Number(rows[0].cnt) !== 1) {
      await client.query("ROLLBACK");
      return NextResponse.json({ error: "Database integrity failure." }, { status: 500 });
    }

    await client.query("COMMIT");
    return NextResponse.json({ success: true });
  } catch (err: unknown) {
    await client.query("ROLLBACK");
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: msg }, { status: 500 });
  } finally {
    client.release();
  }
}
