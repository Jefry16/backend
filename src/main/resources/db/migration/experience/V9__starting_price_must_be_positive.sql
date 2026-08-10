-- starting_price is required and must be greater than zero.
--
-- V7 added it as `NOT NULL DEFAULT 0` with `CHECK (>= 0)`, where 0 carried a
-- second meaning — "not priced yet" — and the storefront hid the badge on it.
-- That meaning is withdrawn: every experience carries a real price, drafts
-- included, so there is no unpriced state to represent and the card always has
-- a figure to show.
--
-- The backfill is not migrating data. Nothing has ever written this column —
-- V7 shipped it unread and unwritten — so every existing row holds the DEFAULT
-- and no operator intent is being overwritten. Rows take the cheapest paid tier
-- across their departures where they have one, which is the closest thing to a
-- real answer available; the rest take a nominal 1.00 that an operator has to
-- replace. Free tiers are excluded from the derivation for the reason V7 gives:
-- a MIN including them would set an infant fare as the headline price.
UPDATE experiences e
   SET starting_price = COALESCE((
           SELECT MIN(a.price)
             FROM slots s
             JOIN audience_slot a ON a.slot_id = s.id
            WHERE s.experience_id = e.id
              AND a.price > 0), 1.00)
 WHERE e.starting_price <= 0;

-- The default goes with the meaning it encoded. An experience without a price
-- is now a 422 at the API, not a row that quietly reads 0.
ALTER TABLE experiences ALTER COLUMN starting_price DROP DEFAULT;

ALTER TABLE experiences DROP CONSTRAINT experiences_starting_price_check;
ALTER TABLE experiences ADD CONSTRAINT experiences_starting_price_check
    CHECK (starting_price > 0);
