-- The media schema is created by FlywayPerDomainConfig. This context FKs into
-- touroperator (the owning operator) and identity (the uploader), so "media" is
-- registered AFTER "touroperator" and "identity" in the DOMAINS list. All
-- objects are qualified with the media schema.

CREATE TABLE media.media (
    id                UUID          NOT NULL PRIMARY KEY,
    tour_operator_id  UUID          NOT NULL REFERENCES touroperator.tour_operators (id),
    storage_key       VARCHAR(600)  NOT NULL CONSTRAINT media_storage_key_unique UNIQUE,
    content_type      VARCHAR(100)  NOT NULL,
    size_bytes        BIGINT        NOT NULL,
    original_name     VARCHAR(500)  NOT NULL,
    -- What a rendered <img> needs beyond its src, and the shape Shopify's image
    -- object carries. All three nullable: nothing populates them yet, and a
    -- non-image (a PDF) has no dimensions to hold.
    --
    -- alt is accessibility and SEO, and cannot be derived later — only the
    -- uploader knows it, so it needs an admin write path.
    --
    -- width/height are for layout shift: an <img> that declares them lets the
    -- browser reserve the box before the file arrives, which is a Core Web
    -- Vitals metric. They must be measured at upload; a theme derives
    -- aspect_ratio from the pair rather than storing a third value.
    alt               VARCHAR(500),
    width             INTEGER       CHECK (width > 0),
    height            INTEGER       CHECK (height > 0),
    created_by        UUID          NOT NULL REFERENCES identity.users (id),
    created_at        TIMESTAMPTZ   NOT NULL
);

-- Backs the tenant-scoped, newest-first cursor list (default sort createdAt DESC,
-- tie-break id). The tenant predicate + sort columns line up with the index.
CREATE INDEX idx_media_operator_created_at
    ON media.media (tour_operator_id, created_at, id);
