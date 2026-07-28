-- metaobject_reference metafields: a reference-typed definition PINS the
-- metaobject type its values may point at (the admin picker lists exactly
-- those entries; validation checks membership). NULL for scalar types.
-- Plain FK (NO ACTION): a pinned metaobject definition cannot be deleted
-- while a metafield definition targets it — surfaced as a 409.
ALTER TABLE metafield.metafield_definitions
    ADD COLUMN metaobject_definition_id UUID REFERENCES metafield.metaobject_definitions(id);
