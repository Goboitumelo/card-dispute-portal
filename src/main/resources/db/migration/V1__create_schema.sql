CREATE TABLE customers (
    id            UUID PRIMARY KEY,
    email         VARCHAR(254) NOT NULL UNIQUE,
    full_name     VARCHAR(150) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Only the last four digits are stored; the full card number is never persisted.
CREATE TABLE cards (
    id          UUID PRIMARY KEY,
    customer_id UUID        NOT NULL REFERENCES customers (id),
    last_four   CHAR(4)     NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_cards_customer ON cards (customer_id);

-- Amounts are stored in minor units (e.g. cents) to avoid floating-point rounding.
CREATE TABLE transactions (
    id            UUID PRIMARY KEY,
    card_id       UUID         NOT NULL REFERENCES cards (id),
    merchant_name VARCHAR(150) NOT NULL,
    amount_minor  BIGINT       NOT NULL,
    currency      CHAR(3)      NOT NULL,
    occurred_at   TIMESTAMPTZ  NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_card_occurred ON transactions (card_id, occurred_at DESC);

CREATE TABLE disputes (
    id                    UUID PRIMARY KEY,
    transaction_id        UUID         NOT NULL REFERENCES transactions (id),
    customer_id           UUID         NOT NULL REFERENCES customers (id),
    reason                VARCHAR(40)  NOT NULL,
    description           VARCHAR(1000) NOT NULL,
    disputed_amount_minor BIGINT       NOT NULL CHECK (disputed_amount_minor > 0),
    status                VARCHAR(30)  NOT NULL CHECK (status IN (
                              'SUBMITTED',
                              'UNDER_REVIEW',
                              'INFO_REQUESTED',
                              'RESOLVED_ACCEPTED',
                              'RESOLVED_REJECTED',
                              'WITHDRAWN')),
    version               BIGINT       NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at            TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Enforces the one-open-dispute-per-transaction rule at database level.
CREATE UNIQUE INDEX uq_disputes_one_open_per_transaction
    ON disputes (transaction_id)
    WHERE status IN ('SUBMITTED', 'UNDER_REVIEW', 'INFO_REQUESTED');

CREATE INDEX idx_disputes_customer_created ON disputes (customer_id, created_at DESC);

CREATE TABLE dispute_status_history (
    id          BIGSERIAL PRIMARY KEY,
    dispute_id  UUID         NOT NULL REFERENCES disputes (id),
    from_status VARCHAR(30),
    to_status   VARCHAR(30)  NOT NULL,
    changed_by  VARCHAR(100) NOT NULL,
    note        VARCHAR(500),
    changed_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_dispute_history_dispute ON dispute_status_history (dispute_id, changed_at);
