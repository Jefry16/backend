-- The operator's brand: Shopify's `shop.brand` object, modelled faithfully.
--
-- Its own 1:1 table rather than seven more columns on tour_operators (already
-- 22 wide): brand is one admin screen with one lifecycle, and two of its parts
-- are collections that cannot be columns at all. The cost is a join on every
-- storefront page, accepted.
--
-- Why columns and not metaobjects, since this looks metaobject-shaped: Shopify's
-- brand assets are a dedicated settings area with fixed fields and their own API,
-- not merchant-defined metafields — the `metafields` key INSIDE brand is the
-- extension point. Our metaobject_definitions are tour_operator_id NOT NULL +
-- created_by, unique on (tour_operator_id, type), so every definition is
-- operator-authored and none is guaranteed to exist. A theme reading
-- shop.brand.slogan off a metaobject would be reading a coincidence, not a
-- contract.
CREATE TABLE touroperator.tour_operator_brand (
    tour_operator_id      UUID PRIMARY KEY
                               REFERENCES touroperator.tour_operators (id) ON DELETE CASCADE,
    slogan                VARCHAR(80),
    short_description     VARCHAR(150),
    -- Bare media ids, no FK: media FKs INTO touroperator, so a reverse FK would
    -- be circular and invert the Flyway domain order (V3 says so for the logo).
    logo_media_id         UUID,
    square_logo_media_id  UUID,
    favicon_media_id      UUID,
    cover_image_media_id  UUID,
    created_at            TIMESTAMPTZ NOT NULL,
    updated_at            TIMESTAMPTZ NOT NULL
);

-- role + an ordered position is what makes the palette faithful: this is exactly
-- Shopify's colors.primary[0].background. position is what the storefront orders
-- by — an unordered palette is a listing that reorders between requests.
CREATE TABLE touroperator.tour_operator_brand_colors (
    tour_operator_id UUID       NOT NULL
                          REFERENCES touroperator.tour_operator_brand (tour_operator_id) ON DELETE CASCADE,
    role             VARCHAR(9) NOT NULL CHECK (role IN ('PRIMARY','SECONDARY')),
    position         SMALLINT   NOT NULL CHECK (position >= 0),
    background       VARCHAR(7) NOT NULL CHECK (background ~ '^#[0-9a-f]{6}$'),
    foreground       VARCHAR(7) NOT NULL CHECK (foreground ~ '^#[0-9a-f]{6}$'),
    PRIMARY KEY (tour_operator_id, role, position)
);

-- Shopify's social links ride brand.metafields; ours are a first-class child
-- table for the reason above — a merchant-authored definition is not a contract
-- a theme can rely on.
--
-- The platform list and BrandSocialPlatform must agree. Widening one without the
-- other is the drift audit_log_actor_type already shipped once, so
-- BrandEnumsMatchTheCheckConstraintsTest reads this constraint and fails the
-- build instead.
CREATE TABLE touroperator.tour_operator_brand_social_links (
    tour_operator_id UUID         NOT NULL
                          REFERENCES touroperator.tour_operator_brand (tour_operator_id) ON DELETE CASCADE,
    platform         VARCHAR(20)  NOT NULL CHECK (platform IN (
        'FACEBOOK','INSTAGRAM','TIKTOK','YOUTUBE','TWITTER',
        'PINTEREST','SNAPCHAT','TUMBLR','VIMEO')),
    url              VARCHAR(500) NOT NULL,
    PRIMARY KEY (tour_operator_id, platform)
);

-- slogan and short_description are content, unlike name and address, which V8
-- excluded on the grounds that a brand name is not. Nullable like every other
-- column here, so a row overlays rather than replaces.
ALTER TABLE touroperator.tour_operator_translations
    ADD COLUMN slogan            VARCHAR(80),
    ADD COLUMN short_description VARCHAR(150);

-- The logo moves under brand: Shopify has no shop.logo, only shop.brand.logo.
-- Every operator gets a brand row carrying the logo it already had, so the admin
-- write path (PUT/DELETE .../logo) follows its column with nothing to migrate at
-- read time.
--
-- The backfill has to run BEFORE the DROP: reversed, it copies a column that no
-- longer exists, and Flyway would leave the schema half-migrated.
INSERT INTO touroperator.tour_operator_brand
    (tour_operator_id, logo_media_id, created_at, updated_at)
SELECT id, logo_media_id, NOW(), NOW()
FROM touroperator.tour_operators;

ALTER TABLE touroperator.tour_operators
    DROP COLUMN logo_media_id;
