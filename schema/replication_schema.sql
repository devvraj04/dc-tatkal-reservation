-- ============================================================
-- DATA REPLICATION AND CONSISTENCY EXPERIMENT SCHEMA
-- ============================================================

BEGIN;

-- ============================================================
-- 1. REPLICATION LOG
-- ============================================================

CREATE TABLE IF NOT EXISTS replication_log (
    id SERIAL PRIMARY KEY,
    replication_id VARCHAR(50) UNIQUE NOT NULL,
    source_node VARCHAR(50) NOT NULL,
    destination_node VARCHAR(50) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    version_number BIGINT NOT NULL,
    lamport_timestamp BIGINT NOT NULL,
    physical_timestamp BIGINT NOT NULL,
    payload TEXT,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    applied_at TIMESTAMP
);

-- Index for ordering by version from a specific source
CREATE INDEX IF NOT EXISTS idx_repl_log_source_version ON replication_log(source_node, version_number);

-- ============================================================
-- 2. REPLICA STATE
-- ============================================================

CREATE TABLE IF NOT EXISTS replica_state (
    id SERIAL PRIMARY KEY,
    node_id INT NOT NULL,
    node_name VARCHAR(50) UNIQUE NOT NULL,
    parent_node_name VARCHAR(50),
    last_applied_version BIGINT DEFAULT 0,
    last_lamport_timestamp BIGINT DEFAULT 0,
    last_sync_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'UP_TO_DATE'
);

COMMIT;
