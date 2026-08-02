-- Per-experience SEO overrides, canonical and per-locale.
--
-- Same widths as page.pages / page.page_translations, for the same reason: the
-- limits are SERP truncation, not a per-context choice.
ALTER TABLE experience.experiences
    ADD COLUMN seo_title       VARCHAR(70),
    ADD COLUMN seo_description VARCHAR(320);

ALTER TABLE experience.experience_translations
    ADD COLUMN seo_title       VARCHAR(70),
    ADD COLUMN seo_description VARCHAR(320);
