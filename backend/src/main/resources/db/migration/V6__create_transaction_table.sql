CREATE TABLE transactions (
    id           UUID         PRIMARY KEY,
    user_id      UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         VARCHAR(16)  NOT NULL,
    amount_cents BIGINT       NOT NULL CHECK (amount_cents > 0),
    category     VARCHAR(100) NOT NULL DEFAULT '',
    description  VARCHAR(500) NOT NULL DEFAULT '',
    date         DATE         NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Listing and summaries are always "my transactions in a date range, newest first".
CREATE INDEX idx_transactions_user_id_date ON transactions (user_id, date DESC);
