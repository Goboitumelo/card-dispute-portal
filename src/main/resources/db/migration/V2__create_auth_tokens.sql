CREATE TABLE auth_tokens (
    token_hash  VARCHAR(64) PRIMARY KEY,
    customer_id UUID        NOT NULL REFERENCES customers (id),
    created_at  TIMESTAMPTZ NOT NULL,
    expires_at  TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_auth_tokens_customer_id ON auth_tokens (customer_id);
