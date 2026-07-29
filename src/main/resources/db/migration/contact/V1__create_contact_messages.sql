-- Shopper-submitted contact-form messages — the store behind the admin
-- Inbox. This slice is ADMIN-ONLY BY DECISION: the write path (the internal
-- intake endpoint the SSR worker will proxy the theme's contact form to,
-- with its rate limit, STOREFRONT audit actor and member alert email)
-- arrives with the storefront arc; until then rows come from the dev seed.
--
-- tour_operator_id is a plain UUID, not a cross-schema FK — denormalized for
-- tenant-scoped listing, and no FK keeps this domain's Flyway position
-- unconstrained. id is UUIDv7 (app-assigned, time-ordered), so
-- ORDER BY id DESC is reverse-chronological and the keyset tiebreaker.
CREATE TABLE contact.contact_messages (
    id               UUID         NOT NULL PRIMARY KEY,
    tour_operator_id UUID         NOT NULL,
    name             VARCHAR(120),                       -- the shopper's name (Shopify themes submit one; optional)
    email            VARCHAR(320) NOT NULL,              -- the shopper's reply address
    summary          VARCHAR(200) NOT NULL,              -- one-line subject
    content          TEXT         NOT NULL,              -- raw body, verbatim (<= 5000, app-enforced at intake)
    read_at          TIMESTAMPTZ,                        -- inbox read-state; NULL = unread
    created_at       TIMESTAMPTZ  NOT NULL
);

-- Tenant inbox, newest first (tenant, id) + the createdAt sort's
-- (tenant, sort_col, id) — the list-endpoint index pattern.
CREATE INDEX idx_contact_messages_tenant_id
    ON contact.contact_messages (tour_operator_id, id);
CREATE INDEX idx_contact_messages_tenant_created_at_id
    ON contact.contact_messages (tour_operator_id, created_at, id);
