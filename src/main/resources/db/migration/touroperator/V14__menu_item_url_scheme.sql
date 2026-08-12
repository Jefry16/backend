-- A menu item's EXTERNAL_URL is rendered into an href on a public page, so an
-- arbitrary scheme accepts javascript:. The domain refuses one from now on
-- (MenuItem -> WebUrl, the same rule SocialUrl uses); this is the other half,
-- because a domain guard only binds the write paths that go through it.
--
-- It is deliberately a CHECK rather than a repair. If a row already violates
-- this, the migration fails at deploy — which is when you want to hear about a
-- javascript: URL sitting in a menu, not later and quietly. Nothing renders
-- menus yet, so there is no window where this is exploitable; there is only the
-- window in which it could be stored.
ALTER TABLE touroperator.menu_items
    ADD CONSTRAINT menu_items_url_scheme_check
        CHECK (url IS NULL OR url ~* '^https?://[^/]');
