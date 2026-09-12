CREATE TABLE chat_messages (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role       VARCHAR(16)  NOT NULL,
    content    TEXT         NOT NULL,
    -- Which tools the assistant ran for this answer, comma-separated names only.
    -- Never the tool inputs or results: those carry the user's personal data.
    tools_used VARCHAR(500) NOT NULL DEFAULT '',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- The conversation is always read as "my messages, in order".
CREATE INDEX idx_chat_messages_user_id_created_at ON chat_messages (user_id, created_at);
