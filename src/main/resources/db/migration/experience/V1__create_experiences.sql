-- The experience schema is created by FlywayPerDomainConfig. Experiences FK into
-- touroperator (owner) + identity (creator), so "experience" is registered AFTER
-- both in the DOMAINS list. media_ids/thumbnail_media_id are BARE uuids (no FK —
-- media already FKs into touroperator; validated at the write boundary via the
-- MediaKeyBatchQuery seam). Slots/pricing/pickup/translations are later slices.

CREATE TABLE experience.experiences (
    id                   UUID          NOT NULL PRIMARY KEY,
    tour_operator_id     UUID          NOT NULL REFERENCES touroperator.tour_operators (id),
    created_by           UUID          NOT NULL REFERENCES identity.users (id),
    handle                 VARCHAR(170)  NOT NULL,
    name                 VARCHAR(200)  NOT NULL,
    description          VARCHAR(500)  NOT NULL,
    long_description     TEXT          NOT NULL,
    featured             BOOLEAN       NOT NULL DEFAULT FALSE,
    tags                 TEXT[]        NOT NULL DEFAULT '{}',
    included             TEXT[]        NOT NULL DEFAULT '{}',
    not_included         TEXT[]        NOT NULL DEFAULT '{}',
    highlights           TEXT[]        NOT NULL DEFAULT '{}',
    media_ids            UUID[]        NOT NULL DEFAULT '{}',
    thumbnail_media_id   UUID,
    duration_minutes     INTEGER       NOT NULL CHECK (duration_minutes > 0),
    booking_cutoff_hours INTEGER       NOT NULL DEFAULT 0 CHECK (booking_cutoff_hours >= 0),
    status               VARCHAR(16)   NOT NULL CHECK (status IN ('DRAFT', 'PUBLISHED')),
    created_at           TIMESTAMPTZ   NOT NULL
);

-- Canonical handle is unique per operator (immutable).
CREATE UNIQUE INDEX experiences_operator_handle_unique
    ON experience.experiences (tour_operator_id, handle);

-- Backs the tenant-scoped, newest-first cursor list.
CREATE INDEX idx_experiences_operator_created_at
    ON experience.experiences (tour_operator_id, created_at, id);
