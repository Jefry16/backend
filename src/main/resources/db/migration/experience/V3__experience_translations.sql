-- Per-locale translation overlays for experiences. One row per (experience,
-- locale); every content column is nullable → falls back to the canonical
-- experience field at render time (a storefront concern, not yet built). `tags`
-- are NOT translated (filter facets). Media is shared, not per-locale.
--
-- tour_operator_id is denormalized here so the localized-slug uniqueness is a
-- per-(operator, locale) constraint without a cross-schema join. The slug is an
-- OPTIONAL localized URL handle (null = use the canonical slug). The
-- slug-history / 301-redirect table is deferred to the storefront slice.

CREATE TABLE experience.experience_translations (
    experience_id     UUID          NOT NULL REFERENCES experience.experiences (id) ON DELETE CASCADE,
    tour_operator_id  UUID          NOT NULL,
    locale            VARCHAR(8)    NOT NULL,
    name              VARCHAR(200),
    description       VARCHAR(500),
    long_description  TEXT,
    highlights        TEXT[],
    included          TEXT[],
    not_included      TEXT[],
    slug              VARCHAR(170),
    PRIMARY KEY (experience_id, locale)
);

-- Optional localized slug is unique per (operator, locale).
CREATE UNIQUE INDEX uq_experience_translations_operator_locale_slug
    ON experience.experience_translations (tour_operator_id, locale, slug)
    WHERE slug IS NOT NULL;
