-- Generic append-only audit trail. One row per business action, written in the
-- SAME transaction as the action it records (via shared.port.AuditTrailPort).
-- No UPDATE or DELETE path exists in the application; rows are history, not state.
--
-- tour_operator_id / actor_id / entity_id are plain UUIDs, NOT cross-schema FKs:
-- an audit row must survive deletion of the entity it describes, and the absence
-- of FKs keeps this domain's position in FlywayPerDomainConfig unconstrained.
--
-- actor_name is FROZEN at write time (the user's display name as it was when
-- the action happened) — historically truthful, filterable like any
-- denormalized list column, and no identity join at read. NULL for SYSTEM.
CREATE TABLE audit.audit_log (
    id               UUID         NOT NULL PRIMARY KEY,   -- UUIDv7, app-assigned: time-ordered, so ORDER BY id DESC is the timeline
    tour_operator_id UUID         NOT NULL,
    actor_type       VARCHAR(20)  NOT NULL,
    actor_id         UUID,                                -- the admin user for USER; NULL for SYSTEM
    actor_name       VARCHAR(255),                        -- frozen display name; NULL for SYSTEM or a name-less account
    entity_type      VARCHAR(40)  NOT NULL,               -- 'EXPERIENCE' | 'AUDIENCE' | 'SLOT' | ...
    entity_id        UUID         NOT NULL,
    action           VARCHAR(80)  NOT NULL,               -- dot-namespaced past tense: 'experience.updated', 'slot.cancelled', ...
    details          JSONB,                               -- action metadata that is NOT a field change, e.g. {"locale": "es"}
    changes          JSONB,                               -- field-level change history: [{"field", "from", "to"}, ...]
    request_id       VARCHAR(64),                         -- MDC requestId for log correlation; NULL off-request
    created_at       TIMESTAMPTZ  NOT NULL,

    -- STOREFRONT joins this list when shopper-side writes (checkout) land.
    CONSTRAINT audit_log_actor_type_check CHECK (actor_type IN ('USER', 'SYSTEM')),
    -- USER entries carry the user id; system entries must not fake one
    CONSTRAINT audit_log_actor_id_presence CHECK ((actor_type = 'USER') = (actor_id IS NOT NULL))
);

-- Per-entity timeline: WHERE tour_operator_id = ? AND entity_type = ? AND entity_id = ? ORDER BY id DESC
CREATE INDEX idx_audit_log_tenant_entity ON audit.audit_log (tour_operator_id, entity_type, entity_id, id);
-- Tenant-wide recent-activity feed; id is also the keyset-cursor tiebreaker
CREATE INDEX idx_audit_log_tenant_recent ON audit.audit_log (tour_operator_id, id);
