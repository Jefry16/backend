-- Per-slot snapshot of the operator's pickup catalog at slot-creation time.
-- pickup_location_id is a BARE uuid (no cross-schema FK): the pickup context
-- writes through shared ports, and the rows are kept in SYNC with the catalog
-- (create backfills, rename/time propagates, delete removes).

CREATE TABLE experience.pickup_location_slot (
    id                    UUID          NOT NULL PRIMARY KEY,
    slot_id               UUID          NOT NULL REFERENCES experience.slots (id) ON DELETE CASCADE,
    pickup_location_id    UUID          NOT NULL,
    pickup_location_name  VARCHAR(200)  NOT NULL,
    pickup_location_time  TIME          NOT NULL,
    CONSTRAINT pickup_location_slot_unique UNIQUE (slot_id, pickup_location_id)
);

CREATE INDEX idx_pickup_location_slot_slot_id ON experience.pickup_location_slot (slot_id);

-- Backs the propagate/remove bulk writes (by pickup id across all slots).
CREATE INDEX idx_pickup_location_slot_pickup_id ON experience.pickup_location_slot (pickup_location_id);
