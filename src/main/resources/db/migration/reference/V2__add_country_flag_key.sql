-- Storage key for each country's flag image (bucket-relative), resolved to a URL
-- at read time via MediaUrlResolver. Nullable — no flags are seeded yet.
ALTER TABLE reference.country ADD COLUMN flag_key VARCHAR(500);
