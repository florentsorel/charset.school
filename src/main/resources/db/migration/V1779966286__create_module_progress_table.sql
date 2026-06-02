-- Per-visitor per-module progression. One row per (token, module_id), the
-- token being the opaque anonymous-visitor id from an HttpOnly cookie.
-- Updated whenever the visitor fully completes an exercise (all steps correct).

CREATE TABLE module_progress (
    id              BIGSERIAL    PRIMARY KEY,
    token           VARCHAR(64)  NOT NULL,
    module_id       VARCHAR(64)  NOT NULL,
    level           SMALLINT     NOT NULL DEFAULT 1,
    streak          INT          NOT NULL DEFAULT 0,
    attempts        INT          NOT NULL DEFAULT 0,
    errors          INT          NOT NULL DEFAULT 0,
    last_played_at  TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (token, module_id)
);
