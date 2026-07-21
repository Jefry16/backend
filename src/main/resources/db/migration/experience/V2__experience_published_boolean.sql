-- Replace the DRAFT/PUBLISHED status enum with a simple `published` boolean.
-- Backfill from the old status before dropping it, so any existing rows keep
-- their state.
ALTER TABLE experience.experiences
    ADD COLUMN published BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE experience.experiences
    SET published = (status = 'PUBLISHED');

ALTER TABLE experience.experiences
    DROP COLUMN status;
