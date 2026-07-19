-- Storage key for each country's flag image (bucket-relative), resolved to a URL
-- at read time via MediaUrlResolver. NOT NULL — every country has a flag key.
-- Add nullable, backfill the seeded countries by convention (flags/{iso2}.svg),
-- then enforce NOT NULL so future inserts must supply one. The referenced file
-- may not exist in the bucket yet; the key is the reference, the asset follows.
ALTER TABLE reference.country ADD COLUMN flag_key VARCHAR(500);

UPDATE reference.country SET flag_key = 'flags/' || lower(code) || '.svg';

ALTER TABLE reference.country ALTER COLUMN flag_key SET NOT NULL;
