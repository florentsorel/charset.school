-- Exercise tracking: relational, table-per-StepType.
--
-- Parent `exercise_attempts` aggregates one full attempt (one exercise) by one
-- user. Parent `attempt_steps` aggregates the micro-questions inside an
-- attempt, with a `step_type` discriminator. Six child tables (one per
-- StepType) hold the type-specific data — `expected` (server-side, never
-- leaves the DB unrevealed) and `user_answer` (filled progressively as the
-- user submits step-by-step via POST /api/exercise/validate).

CREATE TABLE exercise_attempts (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    module_id    VARCHAR(64)  NOT NULL,
    level        SMALLINT     NOT NULL,
    code_point   INT          NOT NULL,
    encoding     VARCHAR(16)  NOT NULL,
    correct      BOOLEAN      NOT NULL DEFAULT FALSE,
    finalized    BOOLEAN      NOT NULL DEFAULT FALSE,
    duration_ms  INT,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_attempts_user_module ON exercise_attempts(user_id, module_id);

-- Parent step table with `step_type` discriminator. `attempts` counts how many
-- times the user has submitted this step (used to escalate hint level on the
-- next wrong submit, and to gate the "give me the answer" button).
CREATE TABLE attempt_steps (
    id          BIGSERIAL    PRIMARY KEY,
    attempt_id  BIGINT       NOT NULL REFERENCES exercise_attempts(id) ON DELETE CASCADE,
    position    SMALLINT     NOT NULL,
    step_type   VARCHAR(32)  NOT NULL,
    correct     BOOLEAN      NOT NULL DEFAULT FALSE,
    error_type  VARCHAR(64),
    attempts    SMALLINT     NOT NULL DEFAULT 0,
    revealed    BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (attempt_id, position)
);

CREATE INDEX idx_attempt_steps_attempt ON attempt_steps(attempt_id);

-- Child tables: one per StepType. PK = step_id (FK to attempt_steps.id).

CREATE TABLE attempt_step_format (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    choices      TEXT[]       NOT NULL,
    expected     VARCHAR(64)  NOT NULL,
    user_answer  VARCHAR(64)
);

CREATE TABLE attempt_step_binary (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    expected     VARCHAR(64)  NOT NULL,
    bit_length   SMALLINT     NOT NULL,
    user_answer  VARCHAR(64)
);

CREATE TABLE attempt_step_bit_groups (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    expected     TEXT[]       NOT NULL,
    user_answer  TEXT[]
);

CREATE TABLE attempt_step_hex_bytes (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    expected     SMALLINT[]   NOT NULL,
    user_answer  SMALLINT[]
);

CREATE TABLE attempt_step_code_point (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    expected     INT          NOT NULL,
    user_answer  INT
);

CREATE TABLE attempt_step_endianness (
    step_id      BIGINT       PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
    expected     VARCHAR(16)  NOT NULL,
    user_answer  VARCHAR(16)
);

CREATE TABLE attempt_step_useful_bit_count (
    step_id     BIGINT PRIMARY KEY REFERENCES attempt_steps (id) ON DELETE CASCADE,
    expected    SMALLINT NOT NULL,
    user_answer SMALLINT
);

-- Child table for StepType.Offset: the UTF-16 supplementary "subtract 0x10000"
-- step. Holds the 20-bit scalar (code point - 0x10000) the user must compute.
CREATE TABLE attempt_step_offset (
                                     step_id      BIGINT  PRIMARY KEY REFERENCES attempt_steps(id) ON DELETE CASCADE,
                                     expected     INT     NOT NULL,
                                     user_answer  INT
);
