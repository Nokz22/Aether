CREATE TABLE events (
    id          UUID          PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title       VARCHAR(300)  NOT NULL,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    starts_at   TIMESTAMPTZ   NOT NULL,
    ends_at     TIMESTAMPTZ   NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- Range queries are always "my events overlapping [from, to)".
CREATE INDEX idx_events_user_id_starts_at ON events (user_id, starts_at);
