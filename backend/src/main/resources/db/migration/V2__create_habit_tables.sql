CREATE TABLE habits (
    id         UUID         PRIMARY KEY,
    user_id    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_habits_user_id ON habits (user_id);

CREATE TABLE habit_checkins (
    habit_id   UUID        NOT NULL REFERENCES habits (id) ON DELETE CASCADE,
    date       DATE        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (habit_id, date)
);
