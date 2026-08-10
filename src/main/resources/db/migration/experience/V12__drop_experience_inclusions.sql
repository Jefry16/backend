-- Drops the "what's included" / "what's not included" lists, canonical and
-- translated.
--
-- The same story as the highlights V11 removed: V1 added them as detail-page
-- copy, V3 gave them a translated overlay, and no reader ever appeared. The
-- storefront card carries name, description, thumbnail, duration and price, and
-- there is no experience detail page.
--
-- All four columns go together, for the reason V11 gives: a translation overlay
-- for a field its owner no longer has is stranded data.
ALTER TABLE experiences DROP COLUMN included;
ALTER TABLE experiences DROP COLUMN not_included;
ALTER TABLE experience_translations DROP COLUMN included;
ALTER TABLE experience_translations DROP COLUMN not_included;
