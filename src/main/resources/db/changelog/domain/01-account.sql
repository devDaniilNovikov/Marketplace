-- liquibase formatted sql
-- changeset market:01-account-table

CREATE TABLE account (
                         id UUID PRIMARY KEY,
                         email VARCHAR(255) NOT NULL UNIQUE,
                         phone VARCHAR(50) UNIQUE,
                         first_name VARCHAR(100),
                         last_name VARCHAR(100),
                         role VARCHAR(50) NOT NULL DEFAULT 'BUYER',
                         created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_account_email ON account (email);