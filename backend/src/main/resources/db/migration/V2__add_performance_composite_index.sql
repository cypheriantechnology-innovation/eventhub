-- Performance composite index for filtered, paginated event queries
CREATE INDEX IF NOT EXISTS idx_events_type_timestamp ON events (event_type, timestamp DESC);
