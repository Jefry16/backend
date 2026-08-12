-- A third owner type: the operator itself, which is Shopify's shop.metafields.
--
-- It is the escape hatch the storefront needs for anything we deliberately did
-- not model — opening hours, a meeting point, the languages the guides speak,
-- a licence number. The alternative was a column per want on a table that is
-- already wide, for facts nothing in the system reasons about.
--
-- Only the CHECK moves. owner_id is already a plain UUID with no cross-schema
-- FK, so an operator-owned value needs no new column: owner_id IS the
-- tour_operator_id, which is also why this owner type needs no ownership seam
-- (the other two ask another context whether the row belongs to the tenant;
-- here the owner and the tenant are the same row).
ALTER TABLE metafield.metafield_definitions
    DROP CONSTRAINT metafield_definitions_owner_type_check;

ALTER TABLE metafield.metafield_definitions
    ADD CONSTRAINT metafield_definitions_owner_type_check
        CHECK (owner_type IN ('EXPERIENCE', 'PAGE', 'TOUR_OPERATOR'));
