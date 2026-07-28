-- Round finding: both indexes duplicate the LEADING column of a unique index
-- on the same table (fields: unique (definition_id, key); values: unique
-- (entry_id, field_definition_id)), so the unique index already serves every
-- lookup they would — they were pure write overhead. NB the superficially
-- similar idx_metafield_values_owner_id (V1) is NOT redundant: owner_id is
-- not a leading column of its table's unique index.
DROP INDEX metafield.idx_metaobject_field_definitions_definition_id;
DROP INDEX metafield.idx_metaobject_entry_values_entry_id;
