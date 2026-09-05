--liquibase formatted sql

--changeset market:00-outbox-table
--comment: Transactional Outbox. Запись идёт в одной локальной ACID-транзакции с бизнес-изменением, распределённых транзакций нет (SCENARIOS.md).
CREATE TABLE market_place.outbox_messages
(
    id             UUID PRIMARY KEY,
    aggregate_type VARCHAR(100)             NOT NULL,
    aggregate_id   UUID                     NOT NULL,
    event_type     VARCHAR(255)             NOT NULL,
    payload        JSONB                    NOT NULL,
    status         VARCHAR(32)              NOT NULL DEFAULT 'PENDING',
    attempts       INT                      NOT NULL DEFAULT 0,
    error_message  TEXT,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    next_retry_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    processed_at   TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_outbox_status CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'DEAD')),
    CONSTRAINT chk_outbox_attempts CHECK (attempts >= 0),
    CONSTRAINT chk_outbox_processed CHECK (processed_at IS NULL OR status IN ('SENT', 'DEAD'))
);
--rollback DROP TABLE IF EXISTS market_place.outbox_messages;

--changeset market:00-outbox-poll-index
--comment: Частичный индекс под воркер SELECT ... FOR UPDATE SKIP LOCKED. Покрывает и первичную отправку, и повторы по next_retry_at. SENT и DEAD в индекс не попадают, поэтому он не растёт вместе с таблицей.
CREATE INDEX idx_outbox_poll ON market_place.outbox_messages (next_retry_at, created_at)
    WHERE status IN ('PENDING', 'FAILED');
--rollback DROP INDEX IF EXISTS market_place.idx_outbox_poll;

--changeset market:00-outbox-aggregate-index
--comment: Поиск истории событий конкретного агрегата: дебаг и сверка.
CREATE INDEX idx_outbox_aggregate ON market_place.outbox_messages (aggregate_type, aggregate_id, created_at DESC);
--rollback DROP INDEX IF EXISTS market_place.idx_outbox_aggregate;
