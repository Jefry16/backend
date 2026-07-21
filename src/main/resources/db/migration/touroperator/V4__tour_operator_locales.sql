-- Per-operator content languages: a primary/default locale on the operator +
-- the set of locales its content supports. The supported set is a child table
-- (not an array) so a future storefront `is_published` flag is a column add, not
-- a restructure. Locale validity is gated against reference.languages at the
-- write boundary (no FK to reference — see UpdateOperatorLocalesUseCase). This
-- is content language, distinct from the admin-UI language on identity.users.

ALTER TABLE touroperator.tour_operators
    ADD COLUMN primary_locale VARCHAR(8) NOT NULL DEFAULT 'en';

CREATE TABLE touroperator.tour_operator_locales (
    tour_operator_id  UUID        NOT NULL REFERENCES touroperator.tour_operators (id),
    locale            VARCHAR(8)  NOT NULL,
    PRIMARY KEY (tour_operator_id, locale)
);

-- Backfill: every existing operator supports at least its primary locale, so the
-- primary-in-supported invariant holds for rows created before this slice.
INSERT INTO touroperator.tour_operator_locales (tour_operator_id, locale)
SELECT id, primary_locale FROM touroperator.tour_operators;
