-- The touroperator schema is created by FlywayPerDomainConfig. This context FKs
-- into identity (the creator/owner) and reference (timezone, currency), so
-- "touroperator" is registered AFTER "identity" and "reference" in the DOMAINS
-- list. All objects are qualified with the touroperator schema.

CREATE TABLE touroperator.tour_operators (
    id           UUID          NOT NULL PRIMARY KEY,
    name         VARCHAR(150)  NOT NULL,
    slug         VARCHAR(170)  NOT NULL CONSTRAINT tour_operators_slug_unique UNIQUE,
    timezone_id  UUID          NOT NULL REFERENCES reference.timezones (id),
    currency_id  UUID          NOT NULL REFERENCES reference.currencies (id),
    address      VARCHAR(500)  NOT NULL,
    created_by   UUID          NOT NULL REFERENCES identity.users (id),
    created_at   TIMESTAMPTZ   NOT NULL,
    updated_at   TIMESTAMPTZ   NOT NULL
);

-- One operator name per owner (case-insensitive) — the DB guard behind the
-- use case's existsByOwnerAndName pre-check + the concurrent-double-submit race.
CREATE UNIQUE INDEX tour_operators_owner_name_unique
    ON touroperator.tour_operators (created_by, lower(name));

CREATE TABLE touroperator.tour_operator_members (
    id                UUID          NOT NULL PRIMARY KEY,
    tour_operator_id  UUID          NOT NULL REFERENCES touroperator.tour_operators (id),
    user_id           UUID          NOT NULL REFERENCES identity.users (id),
    role              VARCHAR(20)   NOT NULL,
    is_default        BOOLEAN       NOT NULL DEFAULT FALSE,
    joined_at         TIMESTAMPTZ   NOT NULL,
    -- Denormalized read-fields: copies of the member's identity name/email,
    -- carried on the pivot so the roster list can sort/filter by them directly.
    -- The list framework sorts only a row's own columns, and identity is a
    -- separate bounded context whose tables can't be joined (§3.5) — so the
    -- read-side copy lives here (the pivot already holds both source ids, making
    -- it the natural home). NOT NULL: every member is a real identity user (FK)
    -- whose name/email are themselves NOT NULL, so the copy is always set at
    -- insert by the member-write use cases and kept current by an identity-change
    -- sync (backfilled in Java via UserAccountQuery — never a cross-schema join).
    -- This is the pattern every list follows to make a cross-context column
    -- sortable/filterable.
    name              VARCHAR(255)  NOT NULL,
    email             VARCHAR(255)  NOT NULL,
    CONSTRAINT tour_operator_members_unique UNIQUE (tour_operator_id, user_id)
);

CREATE INDEX idx_tour_operator_members_user_id
    ON touroperator.tour_operator_members (user_id);

-- Exactly one OWNER per operator — the single-owner invariant, enforced at the
-- DB (not just in code). A concurrent second ownership collides on this key.
CREATE UNIQUE INDEX tour_operator_members_single_owner
    ON touroperator.tour_operator_members (tour_operator_id)
    WHERE role = 'OWNER';

-- At most one default operator per user (the picker's landing choice).
CREATE UNIQUE INDEX tour_operator_members_one_default_per_user
    ON touroperator.tour_operator_members (user_id)
    WHERE is_default = TRUE;

-- Sort/filter the roster by the denormalized name/email within an operator (the
-- list is always tenant-scoped, so lead the index with tour_operator_id).
CREATE INDEX idx_tour_operator_members_name
    ON touroperator.tour_operator_members (tour_operator_id, name);
CREATE INDEX idx_tour_operator_members_email
    ON touroperator.tour_operator_members (tour_operator_id, email);
