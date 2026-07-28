-- Storefront navigation menus, owned by the tour operator (there is exactly
-- one storefront per operator, so no storefront row exists between them —
-- the operator IS the scope). The handle is the stable theme-facing
-- identifier (unique per operator, immutable); items form a tree via the
-- self-referencing parent_id, ordered by position within their parent.
-- The schema allows arbitrary depth; the 3-level cap is enforced in the
-- domain model so every write path inherits it.
CREATE TABLE touroperator.menus (
    id               UUID         NOT NULL PRIMARY KEY,
    tour_operator_id UUID         NOT NULL REFERENCES touroperator.tour_operators(id) ON DELETE CASCADE,
    handle           VARCHAR(170) NOT NULL,
    title            VARCHAR(120) NOT NULL,
    created_by       UUID         NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,

    CONSTRAINT menus_operator_handle_unique UNIQUE (tour_operator_id, handle)
);

-- link_type is an open enum designed to grow additively (CART, METAOBJECT, …
-- once those routes exist) — new values need the CHECK widened plus an enum
-- value, nothing else. resource_id carries the target UUID for typed resource
-- links (EXPERIENCE, PAGE); url the verbatim URL for EXTERNAL_URL. Both
-- nullable, mutually exclusive, shaped by link_type (validated in the domain).
--
-- The parent self-FK is DEFERRABLE INITIALLY DEFERRED: the tree-replace use
-- case inserts a parent and its children in one transaction, and Hibernate's
-- insert batching can flush a child before its parent — a deferred constraint
-- is checked once at COMMIT, so in-tx insert order doesn't matter.
CREATE TABLE touroperator.menu_items (
    id          UUID          NOT NULL PRIMARY KEY,
    menu_id     UUID          NOT NULL REFERENCES touroperator.menus(id) ON DELETE CASCADE,
    parent_id   UUID          REFERENCES touroperator.menu_items(id) ON DELETE CASCADE
                              DEFERRABLE INITIALLY DEFERRED,
    title       VARCHAR(120)  NOT NULL,
    link_type   VARCHAR(30)   NOT NULL,
    resource_id UUID,
    url         VARCHAR(2048),
    position    INTEGER       NOT NULL,
    created_at  TIMESTAMPTZ   NOT NULL,
    updated_at  TIMESTAMPTZ   NOT NULL,

    CONSTRAINT menu_items_link_type_check CHECK (
        link_type IN ('HOME', 'EXPERIENCE_LIST', 'EXPERIENCE', 'PAGE', 'EXTERNAL_URL')
    )
);

CREATE INDEX idx_menu_items_menu_id ON touroperator.menu_items (menu_id);
CREATE INDEX idx_menu_items_parent_id ON touroperator.menu_items (parent_id);

-- Per-locale overrides of an item's title, for the storefront render. Items
-- are rewritten wholesale on every save (fresh ids each time), so these are
-- NOT a standalone resource: they ride inline in the items-replace payload
-- and the items' delete cascades to them.
CREATE TABLE touroperator.menu_item_translations (
    menu_item_id UUID         NOT NULL REFERENCES touroperator.menu_items(id) ON DELETE CASCADE,
    locale       VARCHAR(8)   NOT NULL,
    title        VARCHAR(120) NOT NULL,

    PRIMARY KEY (menu_item_id, locale)
);

-- Every operator gets the two default menus at creation (seeded in
-- CreateTourOperatorUseCase, same transaction as the operator itself).
-- Backfill them for operators that predate this table so "operator without
-- defaults" never exists as a state. The defaults are ordinary menus — the
-- operator may rename or delete them.
INSERT INTO touroperator.menus (id, tour_operator_id, handle, title, created_by, created_at, updated_at)
SELECT gen_random_uuid(), o.id, m.handle, m.title, o.created_by, NOW(), NOW()
FROM touroperator.tour_operators o
CROSS JOIN (VALUES ('main-menu', 'Main menu'), ('footer', 'Footer')) AS m(handle, title);
