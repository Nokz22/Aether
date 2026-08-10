CREATE TABLE projects (
    id          UUID          PRIMARY KEY,
    user_id     UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name        VARCHAR(200)  NOT NULL,
    description VARCHAR(2000) NOT NULL DEFAULT '',
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX idx_projects_user_id ON projects (user_id);

CREATE TABLE tasks (
    id         UUID         PRIMARY KEY,
    project_id UUID         NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    title      VARCHAR(300) NOT NULL,
    status     VARCHAR(16)  NOT NULL,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_tasks_project_id ON tasks (project_id);
