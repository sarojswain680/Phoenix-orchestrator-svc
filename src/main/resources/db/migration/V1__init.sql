CREATE TABLE IF NOT EXISTS jobs (
    id          UUID PRIMARY KEY,
    tenant_id   VARCHAR(255),
    source_type VARCHAR(50),
    source_uri  TEXT,
    status      VARCHAR(50),
    current_step VARCHAR(100),
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);