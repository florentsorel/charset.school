CREATE TABLE users
(
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    name          VARCHAR(255) NOT NULL,
    password_hash VARCHAR(60)  NOT NULL,
    locale        VARCHAR(5)   NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    updated_at    TIMESTAMP
);
