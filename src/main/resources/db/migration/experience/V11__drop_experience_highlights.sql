-- Drops the experience highlight list, canonical and translated.
--
-- V1 added it as bullet copy for a detail page, and V3 gave it a translated
-- overlay. Neither has a reader: the storefront card carries name, description,
-- thumbnail, duration and price, and there is no experience detail page. It has
-- been written, translated, and rendered nowhere.
--
-- Both columns go together. Leaving the translation behind would strand an
-- overlay for a field that no longer exists on the thing it overlays.
ALTER TABLE experiences DROP COLUMN highlights;
ALTER TABLE experience_translations DROP COLUMN highlights;
