-- Operator-owned classification for experiences: "Boat tours", "Walking tours".
--
-- It lives in the experience schema rather than earning a context of its own. A
-- category only ever classifies experiences, so it has no invariants or lifecycle
-- apart from them (LAW §2.2, "default to fewer contexts") — which also makes the
-- reference from experiences a plain intra-schema FK instead of a shared port.
--
-- This is NOT Shopify's `product.category`. Theirs is one node in a global
-- published taxonomy that no merchant edits; this is per-operator and CRUD'd,
-- which is closer to their `product.type` with a stable id behind it.
--
-- The name is unique per operator case-INSENSITIVELY from the first migration.
-- audience/V1 shipped the exact-name index and had to swap it out in V2 when a
-- case-variant duplicate could race past the pre-check; the lesson is applied
-- here rather than relearned.

CREATE TABLE experience.categories (
    id                UUID          NOT NULL PRIMARY KEY,
    tour_operator_id  UUID          NOT NULL REFERENCES touroperator.tour_operators (id),
    name              VARCHAR(80)   NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL
);

CREATE UNIQUE INDEX categories_operator_name_unique
    ON experience.categories (tour_operator_id, lower(name));

-- Backs the tenant-scoped, newest-first cursor list.
CREATE INDEX idx_categories_operator_created_at
    ON experience.categories (tour_operator_id, created_at, id);

-- Per-locale name overlays, mirroring audience_translations column for column:
-- one row per (category, locale), a nullable name that falls back to the
-- canonical one when absent, and a denormalized tour_operator_id so a
-- tenant-scoped operation needs no join.
--
-- Single content column, so there is no partial-translation state to express:
-- the row either exists or it does not, and an upsert that blanks the name
-- deletes the row rather than storing an overlay that changes nothing
-- (PATTERNS §4e).
CREATE TABLE experience.category_translations (
    category_id       UUID         NOT NULL REFERENCES experience.categories (id) ON DELETE CASCADE,
    tour_operator_id  UUID         NOT NULL,
    locale            VARCHAR(8)   NOT NULL,
    name              VARCHAR(80),
    PRIMARY KEY (category_id, locale)
);
