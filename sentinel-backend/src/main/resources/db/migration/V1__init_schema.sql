-- Sentinel AML core schema

CREATE TABLE customers (
    id                          BIGSERIAL PRIMARY KEY,
    customer_id                 VARCHAR(32) NOT NULL UNIQUE,
    first_name                  VARCHAR(100) NOT NULL,
    last_name                   VARCHAR(100) NOT NULL,
    gender                      VARCHAR(10),
    date_of_birth               DATE,
    email                       VARCHAR(255),
    phone_number                VARCHAR(30),
    city                        VARCHAR(100),
    state                       VARCHAR(100),
    country                     VARCHAR(5),
    postal_code                 VARCHAR(20),
    occupation                  VARCHAR(100),
    annual_income                NUMERIC(15, 2),
    marital_status               VARCHAR(30),
    education_level              VARCHAR(50),
    employment_status            VARCHAR(30),
    customer_since               DATE,
    customer_segment             VARCHAR(30),
    kyc_status                   VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    risk_rating                  VARCHAR(10) NOT NULL DEFAULT 'LOW',
    is_politically_exposed        BOOLEAN NOT NULL DEFAULT FALSE,
    preferred_channel            VARCHAR(30),
    email_verified                BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified                 BOOLEAN NOT NULL DEFAULT FALSE,
    num_complaints_last_year      INT NOT NULL DEFAULT 0,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id                          BIGSERIAL PRIMARY KEY,
    account_id                  VARCHAR(32) NOT NULL UNIQUE,
    customer_id                 BIGINT NOT NULL REFERENCES customers (id),
    account_type                VARCHAR(30) NOT NULL,
    account_status               VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    currency                    VARCHAR(3) NOT NULL,
    open_date                   DATE NOT NULL,
    close_date                  DATE,
    branch_code                 VARCHAR(20),
    branch_city                 VARCHAR(100),
    current_balance              NUMERIC(18, 2) NOT NULL DEFAULT 0,
    avg_monthly_balance_6m        NUMERIC(18, 2),
    credit_limit                  NUMERIC(18, 2),
    credit_utilization_pct        NUMERIC(5, 2),
    overdraft_enabled             BOOLEAN NOT NULL DEFAULT FALSE,
    card_type                    VARCHAR(20),
    is_joint_account              BOOLEAN NOT NULL DEFAULT FALSE,
    mobile_banking_enrolled       BOOLEAN NOT NULL DEFAULT FALSE,
    avg_monthly_txn_count         INT,
    account_tier                 VARCHAR(20),
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_accounts_customer ON accounts (customer_id);

CREATE TABLE transactions (
    id                           UUID PRIMARY KEY,
    account_id                   BIGINT NOT NULL REFERENCES accounts (id),
    direction                    VARCHAR(10) NOT NULL,
    amount                       NUMERIC(18, 2) NOT NULL,
    currency                     VARCHAR(3) NOT NULL,
    amount_base_currency          NUMERIC(18, 2) NOT NULL,
    counterparty_name             VARCHAR(200),
    counterparty_account          VARCHAR(50),
    counterparty_country          VARCHAR(5),
    channel                      VARCHAR(30) NOT NULL,
    jurisdiction                 VARCHAR(5),
    status                       VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    txn_timestamp                 TIMESTAMPTZ NOT NULL,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_transactions_account_time ON transactions (account_id, txn_timestamp);
CREATE INDEX ix_transactions_time ON transactions (txn_timestamp);

CREATE TABLE exchange_rates (
    currency_code    VARCHAR(3) PRIMARY KEY,
    rate_to_base     NUMERIC(18, 6) NOT NULL,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE high_risk_jurisdictions (
    country_code    VARCHAR(5) PRIMARY KEY,
    reason          VARCHAR(255),
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    added_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE detection_rules (
    rule_code      VARCHAR(50) PRIMARY KEY,
    name           VARCHAR(150) NOT NULL,
    description    TEXT,
    enabled        BOOLEAN NOT NULL DEFAULT TRUE,
    weight         INT NOT NULL DEFAULT 25,
    config         JSONB NOT NULL DEFAULT '{}'::jsonb,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE alerts (
    id                    UUID PRIMARY KEY,
    customer_id           BIGINT NOT NULL REFERENCES customers (id),
    account_id            BIGINT REFERENCES accounts (id),
    risk_score            INT NOT NULL DEFAULT 0,
    status                VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    dedup_key             VARCHAR(150) NOT NULL,
    disposition_reason    TEXT,
    disposition_actor     VARCHAR(100),
    dispositioned_at       TIMESTAMPTZ,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- One open/in-review alert per (customer, rule pattern) at a time -- de-duplication guard.
CREATE UNIQUE INDEX ux_alerts_open_dedup ON alerts (dedup_key) WHERE status IN ('OPEN', 'IN_REVIEW');
CREATE INDEX ix_alerts_customer ON alerts (customer_id);
CREATE INDEX ix_alerts_status_score ON alerts (status, risk_score DESC);

CREATE TABLE alert_evidence (
    id                          BIGSERIAL PRIMARY KEY,
    alert_id                    UUID NOT NULL REFERENCES alerts (id),
    rule_code                   VARCHAR(50) NOT NULL REFERENCES detection_rules (rule_code),
    risk_contribution            INT NOT NULL,
    explanation                  TEXT NOT NULL,
    evidence_transaction_ids      JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at                    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_alert_evidence_alert ON alert_evidence (alert_id);

CREATE TABLE cases (
    id                  UUID PRIMARY KEY,
    customer_id         BIGINT NOT NULL REFERENCES customers (id),
    status              VARCHAR(30) NOT NULL DEFAULT 'NEW',
    priority            VARCHAR(10) NOT NULL DEFAULT 'MEDIUM',
    assigned_analyst     VARCHAR(100),
    summary             TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE case_alerts (
    case_id     UUID NOT NULL REFERENCES cases (id),
    alert_id    UUID NOT NULL REFERENCES alerts (id),
    PRIMARY KEY (case_id, alert_id)
);

CREATE TABLE audit_log (
    id                BIGSERIAL PRIMARY KEY,
    entity_type       VARCHAR(50) NOT NULL,
    entity_id         VARCHAR(100) NOT NULL,
    action            VARCHAR(50) NOT NULL,
    actor_username    VARCHAR(100) NOT NULL,
    actor_role        VARCHAR(30) NOT NULL,
    details           JSONB,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX ix_audit_log_entity ON audit_log (entity_type, entity_id);

CREATE TABLE users (
    id               BIGSERIAL PRIMARY KEY,
    username         VARCHAR(50) NOT NULL UNIQUE,
    password_hash     VARCHAR(100) NOT NULL,
    full_name         VARCHAR(150),
    role             VARCHAR(30) NOT NULL,
    enabled          BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE ingestion_errors (
    id              BIGSERIAL PRIMARY KEY,
    source          VARCHAR(20) NOT NULL,
    raw_record       TEXT,
    error_message     TEXT NOT NULL,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
