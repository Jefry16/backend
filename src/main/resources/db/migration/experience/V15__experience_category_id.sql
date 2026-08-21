-- An experience's category. Nullable by design, not by migration convenience:
-- an uncategorized experience is a legitimate state, the same one Shopify calls
-- "uncategorized" rather than treating as invalid. Every existing row predates
-- the column and stays valid.
--
-- ON DELETE SET NULL is the decided delete semantic. Removing a category must
-- not remove the experiences filed under it, so they fall back to uncategorized
-- — the state they were in before anyone categorized them.

ALTER TABLE experience.experiences
    ADD COLUMN category_id UUID REFERENCES experience.categories (id) ON DELETE SET NULL;

-- Backs "this operator's experiences in this category", and the SET NULL sweep
-- a category delete performs.
CREATE INDEX idx_experiences_category ON experience.experiences (category_id);
