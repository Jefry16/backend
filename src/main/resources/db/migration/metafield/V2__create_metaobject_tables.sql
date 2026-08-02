-- Metaobjects: free-standing custom content types (size guides, FAQs, guide
-- profiles) an operator defines and fills, living beside metafields in the
-- custom-data context. A DEFINITION is the blueprint (type handle + ordered
-- field definitions reusing the metafield type catalogue); an ENTRY is one
-- piece of content of that type (handle + name + one value per field).

CREATE TABLE metafield.metaobject_definitions (
    id                 UUID           NOT NULL PRIMARY KEY,
    tour_operator_id   UUID           NOT NULL REFERENCES touroperator.tour_operators(id),
    type               VARCHAR(64)    NOT NULL,
    name               VARCHAR(120)   NOT NULL,
    description        VARCHAR(500),
    created_by         UUID           NOT NULL REFERENCES identity.users(id),
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL
);

-- type is lowercase-enforced by the domain value object, so no LOWER() here.
CREATE UNIQUE INDEX metaobject_definitions_operator_type_unique
    ON metafield.metaobject_definitions (tour_operator_id, type);

CREATE INDEX idx_metaobject_definitions_operator_created_at
    ON metafield.metaobject_definitions (tour_operator_id, created_at);

-- The definition's ordered fields. key/type are immutable per field (values
-- are addressed and validated through them); only the display name changes.
-- Removing a field cascades its stored values (below).
CREATE TABLE metafield.metaobject_field_definitions (
    id              UUID           NOT NULL PRIMARY KEY,
    definition_id   UUID           NOT NULL REFERENCES metafield.metaobject_definitions(id) ON DELETE CASCADE,
    key             VARCHAR(64)    NOT NULL,
    type            VARCHAR(30)    NOT NULL,
    name            VARCHAR(120)   NOT NULL,
    position        INT            NOT NULL,
    created_at      TIMESTAMPTZ    NOT NULL,
    updated_at      TIMESTAMPTZ    NOT NULL
);

CREATE UNIQUE INDEX metaobject_field_definitions_definition_key_unique
    ON metafield.metaobject_field_definitions (definition_id, key);

CREATE INDEX idx_metaobject_field_definitions_definition_id
    ON metafield.metaobject_field_definitions (definition_id);

-- Entries. tour_operator_id is denormalized off the definition so the
-- operator-wide cursor list stays tenant-scoped without a join. Deleting a
-- definition cascades its entries (and their values).
CREATE TABLE metafield.metaobject_entries (
    id                 UUID           NOT NULL PRIMARY KEY,
    tour_operator_id   UUID           NOT NULL REFERENCES touroperator.tour_operators(id),
    definition_id      UUID           NOT NULL REFERENCES metafield.metaobject_definitions(id) ON DELETE CASCADE,
    handle             VARCHAR(170)   NOT NULL,
    name               VARCHAR(120)   NOT NULL,
    published          BOOLEAN        NOT NULL DEFAULT FALSE,
    created_by         UUID           NOT NULL REFERENCES identity.users(id),
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL
);

CREATE UNIQUE INDEX metaobject_entries_definition_handle_unique
    ON metafield.metaobject_entries (definition_id, handle);

CREATE INDEX idx_metaobject_entries_operator_created_at
    ON metafield.metaobject_entries (tour_operator_id, created_at);

-- One value per (entry, field). Unset fields simply have no row.
CREATE TABLE metafield.metaobject_entry_values (
    id                    UUID           NOT NULL PRIMARY KEY,
    entry_id              UUID           NOT NULL REFERENCES metafield.metaobject_entries(id) ON DELETE CASCADE,
    field_definition_id   UUID           NOT NULL REFERENCES metafield.metaobject_field_definitions(id) ON DELETE CASCADE,
    value                 TEXT           NOT NULL,
    created_by            UUID           NOT NULL REFERENCES identity.users(id),
    created_at            TIMESTAMPTZ    NOT NULL,
    updated_at            TIMESTAMPTZ    NOT NULL
);

CREATE UNIQUE INDEX metaobject_entry_values_entry_field_unique
    ON metafield.metaobject_entry_values (entry_id, field_definition_id);

CREATE INDEX idx_metaobject_entry_values_entry_id
    ON metafield.metaobject_entry_values (entry_id);
