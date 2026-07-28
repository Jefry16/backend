-- Storefront password protection (Shopify's Preferences → Store access),
-- on the operator row itself — the operator IS the storefront, same call as
-- menus (V5). The password is a shared low-security gate the operator reads
-- back and hands out, NOT a credential — stored plaintext by design (Shopify
-- does the same); enforcement happens on the SSR side when the storefront
-- arc lands. Disabling keeps password + message so re-enabling restores
-- them. password_message is the optional "Message for your visitors" shown
-- on the theme's password page.
ALTER TABLE touroperator.tour_operators
    ADD COLUMN password_enabled    BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN storefront_password VARCHAR(100),
    ADD COLUMN password_message    TEXT;
