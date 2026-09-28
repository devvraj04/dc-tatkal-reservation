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
    const { rows } = await client.query(
      `SELECT passenger_id, passenger_name, age, gender, berth_preference, nationality, id_proof_type, id_proof_number
       FROM passengers
       WHERE user_id = $1
       ORDER BY passenger_id ASC`,
      [userId]
    );

    return NextResponse.json(
      rows.map((r) => ({
        passengerId: Number(r.passenger_id),
        name: r.passenger_name,
        age: r.age,
        gender: r.gender,
        berthPreference: r.berth_preference,
        nationality: r.nationality,
        idProofType: r.id_proof_type,
        idProofNumber: r.id_proof_number,
      }))
    );
  } catch (err: unknown) {
    const msg = err instanceof Error ? err.message : "Unknown error";
    return NextResponse.json({ error: msg }, { status: 500 });
  } finally {
    client.release();
  }
}
