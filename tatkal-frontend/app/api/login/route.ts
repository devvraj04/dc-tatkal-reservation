import { NextRequest, NextResponse } from "next/server";
import pool from "@/lib/db";

export async function POST(req: NextRequest) {
  const { email, password } = await req.json();
  if (!email || !password) {
    return NextResponse.json({ error: "Email and password are required." }, { status: 400 });
  }

  const client = await pool.connect();
  try {
    const { rows } = await client.query(
      `SELECT user_id, full_name, email, password_hash
       FROM users
       WHERE email = $1 AND account_status = 'ACTIVE'`,
      [email]
    );

    if (rows.length === 0 || rows[0].password_hash !== password) {
      return NextResponse.json({ error: "Invalid email or password." }, { status: 401 });
    }

    const user = rows[0];

    await client.query(
      `UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = $1`,
      [user.user_id]
    );

    return NextResponse.json({
      userId: user.user_id,
      fullName: user.full_name,
      email: user.email,
    });
  } catch (err: unknown) {
    console.error("Login error:", err);
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: "Database error: " + msg }, { status: 500 });
  } finally {
    client.release();
  }
}
