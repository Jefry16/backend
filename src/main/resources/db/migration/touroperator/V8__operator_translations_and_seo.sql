-- Shop-level SEO defaults, and the operator's first translation table.
--
-- The SEO fields live here rather than only on content because the HOME page has
-- no content object to carry them, and every other page type falls back to the
-- shop when its own overrides are empty.
--
-- The translation table lands in the same migration deliberately: putting the
-- three columns on tour_operators alone would make shop text untranslatable, the
-- way password_message already was, and undoing that later costs a second
-- migration plus a wire change. password_message moves under the overlay here.
ALTER TABLE touroperator.tour_operators
    ADD COLUMN seo_title         VARCHAR(70),
    ADD COLUMN seo_description   VARCHAR(320),
    -- A bare media id, resolved to a URL at read time (PATTERNS §5) exactly like
    -- logo_media_id. No FK: media is another context's table.
    ADD COLUMN og_image_media_id UUID;

-- Mirrors experience_translations: composite PK on (entity, locale), every
-- content column nullable so a row overlays rather than replaces.
--
-- name, handle and address are deliberately absent: a brand name is not content,
-- and the handle is the URL.
CREATE TABLE touroperator.tour_operator_translations (
    tour_operator_id UUID         NOT NULL REFERENCES touroperator.tour_operators (id) ON DELETE CASCADE,
    locale           VARCHAR(8)   NOT NULL,
    seo_title        VARCHAR(70),
    seo_description  VARCHAR(320),
    password_message TEXT,
    PRIMARY KEY (tour_operator_id, locale)
);
