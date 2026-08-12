-- Replace the DRAFT/PUBLISHED status enum with a `published` boolean, matching
-- experiences (experience/V2). Two ways to say the same thing was one too many:
-- the storefront asks "is this renderable" of both, and a menu item can point at
-- either, so the answer should have one shape.
--
-- Backfill from the old status before dropping it, so existing rows keep their
-- state. The CHECK goes with the column it constrained.
ALTER TABLE page.pages
    ADD COLUMN published BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE page.pages
    SET published = (status = 'PUBLISHED');

ALTER TABLE page.pages
    DROP CONSTRAINT pages_status_check;

ALTER TABLE page.pages
    DROP COLUMN status;
