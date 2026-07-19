-- The identity schema is created by FlywayPerDomainConfig (createSchemas + a
-- dedicated flyway_schema_history_identity). All objects are qualified with the
-- identity schema.

CREATE TABLE identity.users (
    id              UUID            NOT NULL PRIMARY KEY,
    email           VARCHAR(255)    NOT NULL CONSTRAINT users_email_unique UNIQUE,
    name            VARCHAR(255)    NOT NULL,
    hashed_password VARCHAR(255)    NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    -- Storage KEY (users/{userId}/{uuid}-avatar.{ext}), resolved to an absolute
    -- URL at read time via MediaUrlResolver so a base-URL change never requires
    -- a data migration. Nullable — no avatar until one is uploaded.
    avatar_key      VARCHAR(500),
    -- Admin-UI language as a lowercase locale code ("en", "es"). The allowed set
    -- is configuration (app.identity.ui-languages) — adding a language needs no
    -- schema change.
    language        VARCHAR(10)     NOT NULL DEFAULT 'en',
    created_at      TIMESTAMPTZ     NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL
);

CREATE TABLE identity.refresh_tokens (
    id              UUID            NOT NULL PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES identity.users(id),
    token_hash      VARCHAR(64)     NOT NULL,
    family_id       UUID            NOT NULL,
    replaced_by_id  UUID            NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT FALSE,
    expires_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_refresh_tokens_user_id ON identity.refresh_tokens(user_id);
CREATE UNIQUE INDEX idx_refresh_tokens_token_hash ON identity.refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_family_id ON identity.refresh_tokens (family_id);

CREATE TABLE identity.verification_tokens (
    id              UUID            NOT NULL PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES identity.users(id),
    token_hash      VARCHAR(64)     NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_verification_tokens_user_id ON identity.verification_tokens(user_id);
CREATE UNIQUE INDEX idx_verification_tokens_token_hash ON identity.verification_tokens (token_hash);

CREATE TABLE identity.password_reset_tokens (
    id              UUID            NOT NULL PRIMARY KEY,
    user_id         UUID            NOT NULL REFERENCES identity.users(id),
    token_hash      VARCHAR(64)     NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_password_reset_tokens_user_id ON identity.password_reset_tokens(user_id);
CREATE UNIQUE INDEX idx_password_reset_tokens_token_hash ON identity.password_reset_tokens (token_hash);
