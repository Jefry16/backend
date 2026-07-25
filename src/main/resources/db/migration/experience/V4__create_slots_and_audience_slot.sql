-- Slots (bookable departures) + per-slot audience pricing. Times are stored as
-- operator-LOCAL wall-clock (TIMESTAMP without time zone) — no tz conversion at
-- write; storing both start_at and end_at makes a next-day end self-describing
-- and duration derivable, so nothing is inferred from a bare time.
--
-- audience_id / (later) pickup ids are BARE uuids (no FK): a slot's snapshot
-- must outlive a deleted audience. price + capacity are frozen at create; the
-- audience name + pax_per_unit are kept in sync via the snapshot propagator.

CREATE TABLE experience.slots (
    id                      UUID          NOT NULL PRIMARY KEY,
    experience_id           UUID          NOT NULL REFERENCES experience.experiences (id),
    tour_operator_id        UUID          NOT NULL REFERENCES touroperator.tour_operators (id),
    start_at                TIMESTAMP     NOT NULL,
    end_at                  TIMESTAMP     NOT NULL,
    -- Day-of-week of the start (0 = Sunday, 6 = Saturday), derived at create for
    -- indexed "all Mondays" filtering without deriving from the timestamp.
    day                     INTEGER       NOT NULL CHECK (day >= 0 AND day <= 6),
    experience_name         VARCHAR(200)  NOT NULL,
    experience_description   VARCHAR(500)  NOT NULL,
    status                  VARCHAR(16)   NOT NULL CHECK (status IN ('AVAILABLE', 'SOLD_OUT', 'CANCELLED')),
    created_at              TIMESTAMPTZ   NOT NULL
);

-- Default list = tenant-scoped, soonest-first (start_at, id).
CREATE INDEX idx_slots_operator_start_at ON experience.slots (tour_operator_id, start_at, id);
CREATE INDEX idx_slots_experience_start_at ON experience.slots (experience_id, start_at);
CREATE INDEX idx_slots_operator_status ON experience.slots (tour_operator_id, status, id);
CREATE INDEX idx_slots_operator_day ON experience.slots (tour_operator_id, day, id);

CREATE TABLE experience.audience_slot (
    id             UUID           NOT NULL PRIMARY KEY,
    slot_id        UUID           NOT NULL REFERENCES experience.slots (id) ON DELETE CASCADE,
    audience_id    UUID           NOT NULL,
    audience_name  VARCHAR(80)    NOT NULL,
    price          NUMERIC(12,2)  NOT NULL CHECK (price >= 0),
    capacity       INTEGER        NOT NULL CHECK (capacity > 0),
    pax_per_unit   INTEGER        NOT NULL CHECK (pax_per_unit > 0),
    booked_count   INTEGER        NOT NULL DEFAULT 0
                                  CHECK (booked_count >= 0 AND booked_count <= capacity),
    CONSTRAINT audience_slot_unique UNIQUE (slot_id, audience_id)
);

CREATE INDEX idx_audience_slot_slot_id ON experience.audience_slot (slot_id);
