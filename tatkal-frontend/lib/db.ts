import { Pool } from "pg";

const connectionString =
  process.env.DATABASE_URL || "postgresql://postgres@localhost:5433/postgres";

const pool = new Pool({
  connectionString,
  max: 20,
  idleTimeoutMillis: 30000,
  connectionTimeoutMillis: 5000,
});

export default pool;
