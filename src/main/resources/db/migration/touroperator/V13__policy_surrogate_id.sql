-- V12 keyed policies on (tour_operator_id, type), which said "one per type"
-- structurally and needed no surrogate. That was right while the table was read
-- only: nothing listed it.
--
-- The admin write path lists it, and the shared list framework
-- (CriteriaListExecutor) keys its keyset cursor on a UUID `id` and puts that
-- column in the ORDER BY of *every* query, not only paginated ones. An entity
-- without one cannot use the framework at all -- root.get("id") throws while the
-- query is being built. PATTERNS 4b requires the framework for tenant data, so
-- policies get the shape every other listable tenant entity has.
--
-- This is the `menus` shape: a surrogate primary key plus a UNIQUE constraint
-- carrying the real identity. One-per-type is still enforced, by the constraint
-- rather than by the primary key.

ALTER TABLE touroperator.tour_operator_policies
    ADD COLUMN id UUID;

-- Backfill before NOT NULL. These rows are dev-seed only today, and a v4 id is
-- indistinguishable to every reader here: nothing sorts policies by id creation
-- time, the default sort is the type.
UPDATE touroperator.tour_operator_policies
SET id = gen_random_uuid()
WHERE id IS NULL;

ALTER TABLE touroperator.tour_operator_policies
    ALTER COLUMN id SET NOT NULL;

-- The translation table's composite foreign key resolves against the PRIMARY KEY
-- index, so Postgres refuses to drop that key while the FK exists:
--   cannot drop constraint tour_operator_policies_pkey ... because other objects
--   depend on it
-- Found by running it against the real database; the entity mapping alone cannot
-- see it. So the FK is dropped, the key is swapped, and the FK is re-added
-- against the UNIQUE constraint that now carries the same pair. DROP ... CASCADE
-- would also "work" and would silently leave the translations unreferenced.
--
-- V12 created that FK inline, so its name is Postgres-generated; this looks it up
-- rather than hard-coding a name that depends on the server's truncation rules.
DO $$
DECLARE fk_name TEXT;
BEGIN
    SELECT conname INTO fk_name
    FROM pg_constraint
    WHERE conrelid = 'touroperator.tour_operator_policy_translations'::regclass
      AND confrelid = 'touroperator.tour_operator_policies'::regclass
      AND contype = 'f';

    IF fk_name IS NULL THEN
        RAISE EXCEPTION 'expected a foreign key from tour_operator_policy_translations '
                        'to tour_operator_policies, found none';
    END IF;

    EXECUTE format('ALTER TABLE touroperator.tour_operator_policy_translations '
                   'DROP CONSTRAINT %I', fk_name);
END $$;

ALTER TABLE touroperator.tour_operator_policies
    DROP CONSTRAINT tour_operator_policies_pkey;

ALTER TABLE touroperator.tour_operator_policies
    ADD CONSTRAINT tour_operator_policies_operator_type_unique
        UNIQUE (tour_operator_id, type);

ALTER TABLE touroperator.tour_operator_policies
    ADD CONSTRAINT tour_operator_policies_pkey PRIMARY KEY (id);

-- Re-added with an explicit name, and still ON DELETE CASCADE: deleting a policy
-- is how it is unpublished, and its translations must go with it.
ALTER TABLE touroperator.tour_operator_policy_translations
    ADD CONSTRAINT tour_operator_policy_translations_policy_fkey
        FOREIGN KEY (tour_operator_id, type)
        REFERENCES touroperator.tour_operator_policies (tour_operator_id, type)
        ON DELETE CASCADE;
