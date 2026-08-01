# Slice — operator translations + storefront SEO

Executable from a cold start. Read LAW (`../CONSTITUTION.md`), `../MAP.md`,
`CLAUDE.md`, `PATTERNS.md`, `STACK.md` first, then this.

> **Run this slice.** Branch `feat/storefront-seo`. Baseline the suite before you
> start; every count below is compared to it.

## Why

The storefront render envelope needs one resolved `seo` block per page — a title,
a description and a social image, already fallen through their overrides. Today:

- `page` has `seo_title VARCHAR(70)` / `seo_description VARCHAR(320)` on **both**
  `pages` and `page_translations` (page/V1, lines 16-17 and 47-48).
- `experience` has **nothing**, on either table.
- `tour_operators` has **no** SEO fields and **no** social image, so a page whose
  overrides are all empty has nothing to fall back to — and the **home page**,
  which has no content object at all, has no title source but `name`.

The obvious move is three columns on `tour_operators`. **Do not do that.** Shop
text would then be untranslated forever, the way `password_message` already is,
and fixing it later costs a second migration plus a wire change. This slice adds
the translation table first and hangs the SEO fields off it.

## The trap — read before designing anything

`StorefrontOperatorQuery.findBySlug(String slug)` takes **no locale**, and it
cannot: the locale is resolved *from* the operator. `rendering`'s `LocaleResolver`
needs `primaryLocale` + `supportedLocales` to decide which locale the request
gets, so the operator must be loaded before the locale is known.

So do **not** add `findBySlug(slug, locale)` — that forces either two round trips
or locale resolution moving into `touroperator`, which does not own it.

Instead: `findBySlug` returns the canonical operator **plus its translations**,
and `rendering` overlays after `LocaleResolver` has chosen. An operator has a
handful of locales; this is one query, not N.

## Migrations

`touroperator/V8__operator_translations_and_seo.sql`:

```sql
ALTER TABLE touroperator.tour_operators
    ADD COLUMN seo_title         VARCHAR(70),
    ADD COLUMN seo_description   VARCHAR(320),
    ADD COLUMN og_image_media_id UUID;

CREATE TABLE touroperator.tour_operator_translations (
    tour_operator_id UUID         NOT NULL REFERENCES touroperator.tour_operators (id) ON DELETE CASCADE,
    locale           VARCHAR(8)   NOT NULL,
    seo_title        VARCHAR(70),
    seo_description  VARCHAR(320),
    password_message TEXT,
    PRIMARY KEY (tour_operator_id, locale)
);
```

Mirrors `experience_translations`: composite PK on (entity, locale), every content
column nullable so it overlays rather than replaces. No denormalized
`tour_operator_id` copy here — it *is* the key.

**`name`, `slug` and `address` are deliberately not translated.** A brand name is
not content, and the slug is the URL.

`og_image_media_id` is a bare media id resolved to a URL at read, exactly like
`logo_media_id` — PATTERNS §5, never store a URL.

`experience/V8__experience_seo.sql`:

```sql
ALTER TABLE experience.experiences
    ADD COLUMN seo_title       VARCHAR(70),
    ADD COLUMN seo_description VARCHAR(320);

ALTER TABLE experience.experience_translations
    ADD COLUMN seo_title       VARCHAR(70),
    ADD COLUMN seo_description VARCHAR(320);
```

Same widths as `page`, for the same reason (SERP truncation). Match them; do not
invent new limits.

## Backend work

**`touroperator`** — the persistence recipe (PATTERNS §3) for
`TourOperatorTranslation`, plus four use cases mirroring
`Upsert/Get/List/DeleteExperienceTranslationUseCase` one-for-one:

- upsert validates the locale against `OperatorLocalesQuery` (422 if unsupported),
  ADMIN+, audited;
- read is member-visible;
- delete is ADMIN+, audited.

Endpoints — verified against `ExperienceTranslationController` and
`PageTranslationController`, which are identical to each other. The operator *is*
the entity, so there is no nested id segment:

```
@RequestMapping("/api/tour-operators/{tourOperatorId}/translations")
  GET            list every translated locale
  GET    /{locale}
  PUT    /{locale}
  DELETE /{locale}
```

`seo_title`/`seo_description` also gain canonical fields on the operator's own
update path.

**`experience`** — add the two fields to the entity, JPA entity, mappers, the
`ExperienceInput`/request records and the translation upsert. No new use case.

**`rendering`** — a `SeoResolver` in `application/service`, and a `seo` block on
the envelope.

## Resolution — two stages, and keep them separate

Content contexts resolve **their own** chain and may return null. `rendering`
fills the remainder from the operator. `experience` must not learn that operators
have SEO defaults; only `rendering` holds both.

| page | title | description | image |
|---|---|---|---|
| home | shop.seo_title → shop.name | shop.seo_description | shop.og_image → shop.logo |
| experience | tr.seo_title → seo_title → tr.name → name | tr.seo_description → seo_description → tr.description → description → shop.seo_description | thumbnail → shop.og_image → shop.logo |
| page | tr.seo_title → seo_title → tr.title → title | tr.seo_description → seo_description → shop.seo_description | shop.og_image → shop.logo |

Every `shop.*` term above reads the **translated** operator value first, then the
canonical one — that is the whole point of the new table.

A CMS page has no short description, only body HTML, so it skips the content step
rather than truncating markup.

## The envelope

```jsonc
"seo": { "title": "...", "description": "...", "imageUrl": "..." }
```

Top level, not on the content object: **home has no content object**, so anywhere
else and the home page cannot carry SEO at all.

`title` and `description` are fully resolved strings. Do **not** append the shop
name — "X — Acme Tours" is a presentation pattern and the consumer already has
`shop.name`.

## Verification

1. `rm -rf target && ./mvnw -o test` — the `contextLoads` gate runs Flyway against
   real Postgres. Confirm both migrations applied with
   `SELECT version, success FROM touroperator.flyway_schema_history` (and the
   experience one), rather than trusting a green suite.
2. **Mutation-check each fallback step.** A chain is exactly the shape that looks
   tested and is not: with every override populated, removing a step changes
   nothing. Null out one level at a time and assert the next one wins.
3. **Do not probe by adding throwaway migrations.** `contextLoads` applies
   whatever is in `db/migration/`, and a deleted file keeps running from
   `target/classes` — see the tooling traps in `CONTEXT-AUDIT.md`.
4. `pnpm smoke` in the storefront once the envelope changes, since no unit test
   there sees a real payload.

## Deliberately out

`robots`/`noindex`, per-page canonical overrides, OG type, Twitter card fields,
and structured data. None names a caller (LAW §2.4); the storefront serves JSON
and a consumer that needs them can ask.

## Landing

Update `MAP.md` (the `touroperator` and `experience` rows, a ledger entry), and
**delete the Debt entry about shop-level text being untranslated** — this slice is
what closes it. Note in the ledger that `password_message` became translatable as
a side effect, which the storefront's gate copy should start using.
