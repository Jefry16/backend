-- CMS content pages (Shopify "Page" semantics): merchant-authored title/body/
-- SEO served at /pages/{handle}. Content only — the page's theme COMPOSITION
-- belongs to the (future) theme's templates/page[.suffix].json. body is
-- operator-authored raw HTML, size-capped at the API boundary, never sanitized
-- in storage (escaping is a render/consumer concern).
--
-- Deferred with the storefront arc (deliberately absent here): slug-history
-- tables for 301s on renamed handles, and the render-side read ports.
CREATE TABLE page.pages (
    id                 UUID           NOT NULL PRIMARY KEY,
    tour_operator_id   UUID           NOT NULL REFERENCES touroperator.tour_operators(id),
    title              VARCHAR(255)   NOT NULL,
    handle             VARCHAR(170)   NOT NULL,
    body               TEXT           NOT NULL,
    -- SEO overrides (Shopify search-engine-listing): NULL = derive from title/body.
    seo_title          VARCHAR(70),
    seo_description    VARCHAR(320),
    status             VARCHAR(16)    NOT NULL,
    -- Alternate-template assignment: which templates/page.{suffix}.json renders
    -- this page once themes exist. NULL = the base page template.
    template_suffix    VARCHAR(170),
    created_by         UUID           NOT NULL REFERENCES identity.users(id),
    created_at         TIMESTAMPTZ    NOT NULL,
    updated_at         TIMESTAMPTZ    NOT NULL,
    CONSTRAINT pages_status_check CHECK (status IN ('DRAFT', 'PUBLISHED')),
    -- The handle is the page's permanent URL segment, operator-chosen: a
    -- collision is a 409 the operator resolves, never a silent -2 suffix.
    CONSTRAINT pages_tour_operator_id_handle_unique UNIQUE (tour_operator_id, handle)
);

CREATE INDEX idx_pages_operator_created_at
    ON page.pages (tour_operator_id, created_at);

-- Per-locale content overlay — the experience/audience translations shape:
-- every content column nullable (null = untranslated, per-field fallback to
-- canonical at render time); slug = the optional per-locale LOCALIZED handle
-- (no per-field fallback: absent means the canonical handle serves the
-- locale); tour_operator_id denormalized so localized-handle uniqueness stays
-- a single-schema check.
CREATE TABLE page.page_translations (
    page_id            UUID           NOT NULL REFERENCES page.pages(id) ON DELETE CASCADE,
    locale             VARCHAR(8)     NOT NULL,
    tour_operator_id   UUID           NOT NULL,
    slug               VARCHAR(170),
    title              VARCHAR(255),
    body               TEXT,
    seo_title          VARCHAR(70),
    seo_description    VARCHAR(320),
    PRIMARY KEY (page_id, locale)
);

CREATE UNIQUE INDEX uq_page_translations_operator_locale_slug
    ON page.page_translations (tour_operator_id, locale, slug)
    WHERE slug IS NOT NULL;
