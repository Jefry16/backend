-- Per-locale name overlays for audiences. One row per (audience, locale); a
-- null name means untranslated → falls back to the canonical audience name at
-- render time. tour_operator_id is denormalized for symmetry with
-- experience_translations (tenant-scoped operations without a join).

CREATE TABLE audience.audience_translations (
    audience_id       UUID         NOT NULL REFERENCES audience.audiences (id) ON DELETE CASCADE,
    tour_operator_id  UUID         NOT NULL,
    locale            VARCHAR(8)   NOT NULL,
    name              VARCHAR(80),
    PRIMARY KEY (audience_id, locale)
);
