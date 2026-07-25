-- The pickup schema is created by FlywayPerDomainConfig. Pickup locations FK
-- into touroperator (owner) + identity (creator), so "pickup" is registered
-- AFTER both in the DOMAINS list. Slots snapshot this catalog into
-- experience.pickup_location_slot by BARE id and are kept in SYNC (create
-- backfills, rename/time propagates, delete removes) — unlike audience pricing,
-- whose per-slot price/capacity stay frozen.

CREATE TABLE pickup.pickup_locations (
    id                UUID          NOT NULL PRIMARY KEY,
    tour_operator_id  UUID          NOT NULL REFERENCES touroperator.tour_operators (id),
    created_by        UUID          NOT NULL REFERENCES identity.users (id),
    name              VARCHAR(200)  NOT NULL,
    -- Time-of-day a shopper meets at this point (operator-local, e.g. 09:30).
    -- Quoted: "time" is a Postgres type keyword.
    "time"            TIME          NOT NULL,
    created_at        TIMESTAMPTZ   NOT NULL
);

-- Name is unique per operator case-insensitively (create/update duplicate → 409).
CREATE UNIQUE INDEX pickup_locations_operator_name_unique
    ON pickup.pickup_locations (tour_operator_id, lower(name));

-- Backs the tenant-scoped, newest-first cursor list.
CREATE INDEX idx_pickup_locations_operator_created_at
    ON pickup.pickup_locations (tour_operator_id, created_at, id);
