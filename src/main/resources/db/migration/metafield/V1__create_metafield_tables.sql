-- Metafield definitions: the schema of one custom field an operator attaches
-- to a resource kind (v1 owner types: EXPERIENCE, PAGE — both exist in the
-- rebuild, so both ship from day one). Identified by namespace.key per
-- (operator, owner type); namespace/key are handle-shaped so themes can later
-- address them with Liquid dot access (resource.metafields.namespace.key).
CREATE TABLE metafield.metafield_definitions (
    id                 UUID           NOT NULL PRIMARY KEY,
    tour_operator_id   UUID           NOT NULL REFERENCES touroperator.tour_operators(id),
    owner_type         VARCHAR(30)    NOT NULL,
    namespace          VARCHAR(64)    NOT NULL,
    key                VARCHAR(64)    NOT NULL,
    type               VARCHAR(30)    NOT NULL,
    name               VARCHAR(120)   NOT NULL,
    description        VARCHAR(500),
    created_by         UUID           NOT NULL REFERENCES identity.users(id),
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL,
    CONSTRAINT metafield_definitions_owner_type_check
        CHECK (owner_type IN ('EXPERIENCE', 'PAGE'))
);

-- namespace/key are lowercase-enforced by the domain value objects, so the
-- uniqueness backstop needs no LOWER() wrapping.
CREATE UNIQUE INDEX metafield_definitions_operator_owner_namespace_key_unique
    ON metafield.metafield_definitions (tour_operator_id, owner_type, namespace, key);

CREATE INDEX idx_metafield_definitions_operator_created_at
    ON metafield.metafield_definitions (tour_operator_id, created_at);

-- Values: the content one definition holds on one owning resource instance.
-- At most one value per (definition, owner). owner_id is a plain UUID, not a
-- cross-schema FK — a deleted experience/page leaves rows no read path can
-- reach (reads join through the operator-scoped definition and take the owner
-- id from an ownership-checked request).
CREATE TABLE metafield.metafield_values (
    id              UUID           NOT NULL PRIMARY KEY,
    definition_id   UUID           NOT NULL REFERENCES metafield.metafield_definitions(id) ON DELETE CASCADE,
    owner_id        UUID           NOT NULL,
    value           TEXT           NOT NULL,
    created_by      UUID           NOT NULL REFERENCES identity.users(id),
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL
);

CREATE UNIQUE INDEX metafield_values_definition_id_owner_id_unique
    ON metafield.metafield_values (definition_id, owner_id);

-- Backs the per-resource list read (join to definitions on definition_id).
CREATE INDEX idx_metafield_values_owner_id
    ON metafield.metafield_values (owner_id);
