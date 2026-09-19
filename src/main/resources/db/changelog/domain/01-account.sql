--liquibase formatted sql

--changeset market:01-account-table
--comment: V4 SSOT. Таблица public.account с колонкой role заменена целиком — прод-данных нет. id приходит из Keycloak, без default. Снапшоты имён VARCHAR(255) — как в Keycloak, иначе длинное имя роняло бы консьюмер USER_UPDATED.
CREATE TABLE market_place.accounts
(
    id                              UUID PRIMARY KEY,
    user_name                       VARCHAR(255)             NOT NULL,
    business_status                 VARCHAR(32)              NOT NULL DEFAULT 'BUYER',
    banned                          BOOLEAN                  NOT NULL DEFAULT FALSE,
    email_snapshot                  VARCHAR(255),
    first_name_snapshot             VARCHAR(255),
    last_name_snapshot              VARCHAR(255),
    rejection_reason                TEXT,
    seller_applications             SMALLINT                 NOT NULL DEFAULT 0,
    seller_application_hold_until   TIMESTAMP WITH TIME ZONE,
    version                         BIGINT                   NOT NULL DEFAULT 0,
    created_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at                      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at                      TIMESTAMP WITH TIME ZONE,
    CONSTRAINT chk_accounts_business_status CHECK (business_status IN ('BUYER', 'SELLER_PENDING', 'SELLER', 'SELLER_REJECTED')),
    CONSTRAINT chk_accounts_seller_applications CHECK (seller_applications >= 0),
    CONSTRAINT chk_accounts_deleted_snapshots CHECK (
        deleted_at IS NULL
            OR (email_snapshot IS NULL AND first_name_snapshot IS NULL AND last_name_snapshot IS NULL)
        )
);
--rollback DROP TABLE IF EXISTS market_place.accounts;

--changeset market:01-account-indexes
--comment: Частичные UNIQUE: soft-deleted username и email можно переиспользовать.
CREATE UNIQUE INDEX ux_accounts_user_name_active
    ON market_place.accounts (user_name)
    WHERE deleted_at IS NULL;

CREATE UNIQUE INDEX ux_accounts_email_snapshot_active
    ON market_place.accounts (email_snapshot)
    WHERE deleted_at IS NULL AND email_snapshot IS NOT NULL;

CREATE INDEX ix_accounts_business_status
    ON market_place.accounts (business_status)
    WHERE deleted_at IS NULL;
--rollback DROP INDEX IF EXISTS market_place.ix_accounts_business_status; DROP INDEX IF EXISTS market_place.ux_accounts_email_snapshot_active; DROP INDEX IF EXISTS market_place.ux_accounts_user_name_active;

--changeset market:01-account-updated-at-trigger splitStatements:false
--comment: updated_at ставит БД, а не JVM.
CREATE TRIGGER trg_accounts_set_updated_at
    BEFORE UPDATE ON market_place.accounts
    FOR EACH ROW
    EXECUTE FUNCTION market_place.set_updated_at();
--rollback DROP TRIGGER IF EXISTS trg_accounts_set_updated_at ON market_place.accounts;
