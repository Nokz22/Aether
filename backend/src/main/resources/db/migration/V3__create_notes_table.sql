CREATE TABLE notes (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    title      VARCHAR(200) NOT NULL DEFAULT '',
    content    TEXT         NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Lists are always "my notes, most recently edited first".
CREATE INDEX idx_notes_user_id_updated_at ON notes (user_id, updated_at DESC);
