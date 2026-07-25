-- The audience schema is created by FlywayPerDomainConfig. Audiences FK into
-- touroperator (owner) + identity (creator), so "audience" is registered AFTER
-- both in the DOMAINS list. Slots (in the experience schema) will reference an
-- audience by a BARE id (no FK) and snapshot its name + price, so an audience
-- delete never cascades into existing slot pricing — the frozen snapshot survives.

CREATE TABLE audience.audiences (
    id                UUID          NOT NULL PRIMARY KEY,
    tour_operator_id  UUID          NOT NULL REFERENCES touroperator.tour_operators (id),
    created_by        UUID          NOT NULL REFERENCES identity.users (id),
    name              VARCHAR(80)   NOT NULL,
    pax_per_unit      INTEGER       NOT NULL CHECK (pax_per_unit > 0),
    created_at        TIMESTAMPTZ   NOT NULL
);

-- Audience name is unique per operator (the create/update duplicate → 409).
CREATE UNIQUE INDEX audiences_operator_name_unique
    ON audience.audiences (tour_operator_id, name);

-- Backs the tenant-scoped, newest-first cursor list.
CREATE INDEX idx_audiences_operator_created_at
    ON audience.audiences (tour_operator_id, created_at, id);
