-- Per-locale overlays for metafield VALUES. Until now `metafield` was the only
-- context with operator-authored content and no translation table, so an
-- operator whose primary locale is Spanish wrote `custom.opening-hours` once and
-- the English visitor read it in Spanish.
--
-- The overlay is ROW-shaped, not column-shaped. Every other translation table
-- here is nullable columns falling back per field (touroperator/V8,
-- experience/V3, page/V1); a metafield value is one row with one value, so
-- `value` is NOT NULL and "no row" IS the fallback. A row holding a blank would
-- mean the same as no row while looking like a translated field in the editor —
-- the reason MetafieldValueValidator already refuses a blank and sends you to
-- DELETE.
--
-- Only text types are translatable (single_line_text, multi_line_text) and the
-- use case enforces it: a translated `true` is `true` and a translated
-- 2026-08-13 is the same date. That rule cannot be a CHECK here — the type lives
-- on metafield_definitions, two tables away.

CREATE TABLE metafield.metafield_value_translations (
    metafield_value_id UUID        NOT NULL REFERENCES metafield.metafield_values (id) ON DELETE CASCADE,
    locale             VARCHAR(8)  NOT NULL,
    value              TEXT        NOT NULL,
    created_by         UUID        NOT NULL REFERENCES identity.users (id),
    created_at         TIMESTAMPTZ NOT NULL,
    updated_at         TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (metafield_value_id, locale)
);

-- The storefront reads one locale across all of an owner's values, and the
-- editor lists the locales one value has. The primary key serves the first;
-- this serves neither on its own, but the FK's cascade delete does a
-- value-id lookup on every value clear.
CREATE INDEX idx_metafield_value_translations_locale
    ON metafield.metafield_value_translations (locale);
