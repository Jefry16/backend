-- The operator's postal address, in parts.
--
-- The old column was one free-text line ('Calle Mayor 1, 28013 Madrid'). It is
-- DROPPED, not migrated: a line like that cannot be split into street/city/zip
-- reliably, and guessing would put wrong data in fields the storefront
-- publishes. Three operators exist and they are all ours, so they re-enter it.
--
-- Every column is nullable because the drop leaves every existing row without an
-- address, and the database cannot demand what nobody has. Required-ness lives
-- in the API: create demands address1 + city + country, and a PATCH that
-- supplies an address supplies a whole one.
--
-- The CHECK is what stops those six nullable columns representing five illegal
-- states. A half-written address — a city with no street, a zip with no country
-- — is exactly the shape that makes one endpoint 422 while its neighbour
-- answers 200, because the value object runs on the detail read and not on a
-- projection. Same guard as V14's menu url: the table holds the rule, not only
-- the write path that happens to go through the domain today.
ALTER TABLE touroperator.tour_operators
    ADD COLUMN address1   VARCHAR(255),
    ADD COLUMN address2   VARCHAR(255),
    ADD COLUMN city       VARCHAR(120),
    ADD COLUMN province   VARCHAR(120),
    ADD COLUMN zip        VARCHAR(20),
    ADD COLUMN country_id UUID REFERENCES reference.country (id),
    DROP COLUMN address;

ALTER TABLE touroperator.tour_operators
    ADD CONSTRAINT tour_operators_address_all_or_nothing CHECK (
        (address1 IS NULL AND address2 IS NULL AND city IS NULL
            AND province IS NULL AND zip IS NULL AND country_id IS NULL)
        OR (address1 IS NOT NULL AND city IS NOT NULL AND country_id IS NOT NULL));
