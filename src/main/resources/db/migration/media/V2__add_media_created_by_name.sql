-- Denormalize the uploader's name onto each media row so the library can
-- sort/filter by "who uploaded" off the single root — a cross-context field
-- (identity) otherwise resolved post-pagination, which can't be sorted (§3.5).
-- Frozen snapshot: set at upload time; a later rename does not rewrite old rows.
--
-- Forward migration (production-safe): add nullable, backfill existing rows from
-- the uploader's current identity name, then enforce NOT NULL. created_by is
-- NOT NULL and FKs into identity.users, so every existing row backfills.

ALTER TABLE media.media
    ADD COLUMN created_by_name VARCHAR(255);

UPDATE media.media m
   SET created_by_name = u.name
  FROM identity.users u
 WHERE u.id = m.created_by;

ALTER TABLE media.media
    ALTER COLUMN created_by_name SET NOT NULL;
