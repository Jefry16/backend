-- The shop's public contact details, for the storefront footer.
--
-- Both nullable: every existing operator predates them, and an operator who
-- publishes neither still has a storefront. Nullable also matters downstream —
-- the templates guard these with a section, and Mustache treats '' as truthy, so
-- a blank must never be stored where a NULL is meant.
--
-- Deliberately NOT in tour_operator_translations, for the reason V8 gives for
-- name and address: a phone number and a mailbox exist in one form, not one per
-- language.
--
-- No format CHECK. Shape is the write path's business and there is no write path
-- yet; a constraint here would be a validation rule with no validator behind it,
-- and E.164-vs-national is a product decision nobody has made.
ALTER TABLE touroperator.tour_operators
    ADD COLUMN phone VARCHAR(30),
    -- 320 = RFC 5321's 64-octet local part + '@' + 255-octet domain, matching
    -- contact_messages.email. identity.users.email is the older 255.
    ADD COLUMN email VARCHAR(320);
