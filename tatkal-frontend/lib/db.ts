import { Pool } from "pg";

const connectionString =
  process.env.DATABASE_URL ||
  process.env.REMOTE_DATABASE_URL ||
  "postgresql://postgres.tigpgunzlteutfykaeqd:DCtatkal2026@aws-0-ap-south-1.pooler.supabase.com:6543/postgres";

// Clean any prepared statement or pg-connection-string sslmode flags
const cleanUrl = connectionString
  .replace(/([?&])sslmode=[^&]+(&|$)/, "$1")
  .replace(/[?&]$/, "");

const pool = new Pool({
  connectionString: cleanUrl,
  ssl: {
    rejectUnauthorized: false,
  },
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 10000,
});

export default pool;
