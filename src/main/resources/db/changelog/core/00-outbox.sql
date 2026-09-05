-- liquibase formatted sql
-- changeset market:00-outbox-table

CREATE TABLE outbox_messages (
                                 id UUID PRIMARY KEY,
                                 aggregate_type VARCHAR(100) NOT NULL,
                                 aggregate_id VARCHAR(255) NOT NULL,
                                 event_type VARCHAR(255) NOT NULL,
                                 payload JSONB NOT NULL,
                                 status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
                                 error_message TEXT,
                                 created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_outbox_pending ON outbox_messages (created_at) WHERE status = 'PENDING';