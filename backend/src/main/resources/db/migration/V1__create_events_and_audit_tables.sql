CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    description VARCHAR(500) NOT NULL,
    timestamp TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_events_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_events_timestamp_desc ON events (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_events_event_type ON events (event_type);

CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY,
    event_id VARCHAR(100) NOT NULL,
    action VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_event_id ON audit_logs (event_id);
