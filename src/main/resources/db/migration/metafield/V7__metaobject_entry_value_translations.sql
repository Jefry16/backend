-- Per-locale overlays for METAOBJECT ENTRY field values — slice 2 of the work
-- V6 started. Without this the seeded boat's `notes` reads Spanish on every
-- language of the site, even though the metafield pointing at it translates.
--
-- Identical in shape to metafield_value_translations, and deliberately so: a
-- metaobject field reuses the same type catalogue, so it gets the same
-- row-shaped overlay (`value` NOT NULL, "no row" is the fallback) and the same
-- text-only rule, enforced in the use case because the type lives on
-- metaobject_field_definitions rather than here.

CREATE TABLE metafield.metaobject_entry_value_translations (
    entry_value_id UUID        NOT NULL REFERENCES metafield.metaobject_entry_values (id) ON DELETE CASCADE,
    locale         VARCHAR(8)  NOT NULL,
    value          TEXT        NOT NULL,
    created_by     UUID        NOT NULL REFERENCES identity.users (id),
    created_at     TIMESTAMPTZ NOT NULL,
    updated_at     TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (entry_value_id, locale)
);

CREATE INDEX idx_metaobject_entry_value_translations_locale
    ON metafield.metaobject_entry_value_translations (locale);
