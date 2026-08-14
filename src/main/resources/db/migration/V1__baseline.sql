-- Baseline migration for system setup
CREATE TABLE IF NOT EXISTS system_metadata (
    key VARCHAR(64) PRIMARY KEY,
    value VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

INSERT INTO system_metadata (key, value)
VALUES ('schema_version', '1.0.0'), ('platform_name', 'Odoo Cafe POS Backend')
ON CONFLICT (key) DO NOTHING;
