-- Per-user per-module progression. One row per (user_id, module_id).
-- Updated whenever the user fully completes an exercise (all steps correct).

CREATE TABLE module_progress (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id       VARCHAR(64)  NOT NULL,
    level           SMALLINT     NOT NULL DEFAULT 1,
    streak          INT          NOT NULL DEFAULT 0,
    attempts        INT          NOT NULL DEFAULT 0,
    errors          INT          NOT NULL DEFAULT 0,
    last_played_at  TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, module_id)
);
