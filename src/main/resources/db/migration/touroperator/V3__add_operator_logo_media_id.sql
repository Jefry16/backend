-- The operator's logo is a reference to one of its own media records, held as a
-- bare media id (nullable). It is intentionally NOT a foreign key: media FKs INTO
-- touroperator (media.tour_operator_id → tour_operators), so a reverse FK would be
-- circular and would also invert the Flyway domain order. Ownership + existence
-- are enforced at the write boundary (SetOperatorLogoUseCase validates the id via
-- the media MediaKeyBatchQuery); a since-deleted logo resolves to no URL at read.

ALTER TABLE touroperator.tour_operators
    ADD COLUMN logo_media_id UUID;
