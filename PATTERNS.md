# Vointika Backend — Patterns

The **how we build** reference: the concrete shapes that have repeated across
slices. Read this at LAW §5.2 ("look before you build") — a new slice follows
the matching recipe here instead of reverse-engineering existing code or copying
the archive.

Companion to `CONSTITUTION.md` (the *rules*) and `STACK.md` (the *versions*).
This file is *recipes*, not law.

**How this stays lean:** a pattern earns a spot only when it's **repeated ≥2×
and stable**. Deviate deliberately (LAW §2.3 — structure follows need), and when
you do, either it's a one-off (don't touch this file) or it's the new pattern
(update the recipe). Prune anything that stops being true.

---

## 1. Context shapes

Every bounded context is one of three shapes. The layer DAG
(`domain ← application ← {infrastructure, presentation}`) and cross-context
isolation are ArchUnit-enforced; `domain` stays pure (no Spring/JPA/Jackson).

- **Full context** — owns entities and an HTTP surface. Layers:
  `domain / application / infrastructure / presentation`.
  Canonical: `identity`, `reference`.
- **Worker module** — reacts to events, owns no entities and no HTTP. Layers:
  `application / infrastructure` only (no `domain`, no `presentation`).
  Canonical: `notification`.
- **Read surface** — owns an HTTP surface and **no entities**: every row it
  renders belongs to another context and arrives through a shared query port.
  Layers: `application / infrastructure / presentation`, no `domain`.
  Canonical: `storefront`.

**The layer DAG bites hardest in the shape with no domain.** `presentation` and
`infrastructure` may not reach each other. So a helper that the *controller* uses
and the *config* wires cannot live in either one. Put it in `application` as a
plain POJO and `@Bean` it from the context's config.

`storefront`'s `TenantHandleResolver` was written in `infrastructure/web` first
and ArchUnit rejected it in three places. It takes the host as a `String` and
knows nothing about servlets, so `application/policy` is where it belonged
anyway.

**`application/policy` is now the settled home for that kind of rule** —
`TenantHandleResolver` (host → tenant) lives there. A policy that holds
configuration is an instance, `@Bean`ed from the config — `TenantHandleResolver`
takes the base domain, so it is one. A pure function of its arguments is `static`
with no bean at all. The only question is whether there is state to inject.

**A constant that two layers need also goes here.** `StorefrontRoutes` holds
every storefront path for exactly that reason: the controller that maps them and
the config that registers them as public cannot see each other.

`shared` and `reference` are shared kernels — importable by any context.
Everything else is isolated: a context reaches another only via a shared query
port or an event (never a direct import).

## 2. What goes in each layer

- `domain/entity` — aggregates, pure, behavior-rich (mutators bump `updatedAt`).
  `domain/valueobject` — records that validate in their constructor
  (`Email`, `Password`). `domain/enums`. `domain/repository` — the outbound
  repository *interfaces* the use cases depend on.
- `application/usecase` — one class per operation, a **plain constructor-injected
  POJO** (no Spring annotations — keeps the layer framework-free).
  `application/dto/input` + `dto/output`. `application/port` — outbound port
  interfaces the use case needs (impl lives in `infrastructure`).
- `infrastructure/config` — the context's `UseCaseConfig` (`@Bean`-wires every
  use case by hand) + its `@ConfigurationProperties` records.
  `infrastructure/persistence/{entity,mapper,repository}`. `infrastructure/port`
  — adapter impls of application/shared ports. `infrastructure/query` — impls of
  shared query ports this context provides. `infrastructure/security`,
  `infrastructure/consumer` (workers).
- `presentation/{controller,request,response}`, plus `presentation/view` where a
  context server-renders: a template's context object is not a serialized JSON
  response, and calling it one would mislead. There is no live example while the
  storefront is a placeholder — §2a records the shape to rebuild.

## 2a. The render envelope (a server-rendered page's context object)

> **This is the storefront's data contract, and it is live and served as JSON** —
> the globals on `/` and `/{locale}`, and the globals plus `page` on
> `/pages/{handle}`. JSON and not HTML on purpose: the contract is settled before
> anything is server-rendered again, because a wrong field is visible in a body and
> invisible under markup nobody reads yet.
>
> Four addresses still answer a placeholder `{handle, status}`: the experiences
> listing and the policy page, each with and without a locale prefix. No template
> exists either, so the theme object model as a *render* context is the part that
> is still only a plan.
>
> Each rule below carries its own reason:
>
> - **There is no `page` object in the globals.** Shopify's `page` is a CMS page
>   and so is ours. The current page's metadata is therefore `pageTitle` /
>   `pageDescription` / `ogImageUrl` at the top level, matching their
>   `page_title`/`page_description` globals. That leaves the name free for
>   `/pages/{handle}`.
> - **camelCase, not Liquid's snake_case**, so the storefront reads like the rest
>   of this codebase rather than like Shopify.
> - **`localization` mirrors Shopify's shape**: `language` is the one being
>   served, and each entry carries `primary` (is it the operator's default) plus both
>   names — `name` in the operator's primary locale, `endonymName` in its own. Both
>   are derived from the JDK's CLDR data rather than curated, because Shopify's
>   `name` is one value per *pair* of languages.
> - **There is no key-side copy of the operator.** The use case carries the shared
>   port's `TourOperatorView` directly; a field-for-field application DTO beside it would
>   be the identical-pair shape §4c leaves undecided.
> - **`canonicalUrl` and `pageType` are top-level** (2026-08-13). Shopify writes
>   the head itself through `content_for_header`; nothing writes ours, so the
>   values a theme needs for `<link rel="canonical">` and JSON-LD are served
>   rather than assumed. `pageType` is Shopify's `request.page_type` flattened
>   out — the rest of that object is either theme-editor state (`design_mode`,
>   `visual_preview_mode`) or already served (`origin` is `tourOperator.url`,
>   `locale` is `localization.language`). **The canonical is always
>   self-referencing**, which falls out of `LocaleRule` rather than being a
>   choice: the primary locale lives at the bare path and `/{primary}` is a 404,
>   so one page in one language has exactly one address. **Build it from the
>   resolved locale and handle, never off the request URI** — echoing the URI
>   back repeats the query string, which is the one thing the tag exists to
>   strip. Each route names its own `pageType` at the `from` overload that builds
>   it; inferring it from "which objects are present" makes any future
>   object-less route the index by accident.
> - **Metafield values and metaobject entry fields carry per-locale overlays**
>   (2026-08-13/14). Text types only
>   (`single_line_text`, `multi_line_text`): a translated `true` is `true`, and a
>   `metaobject_reference` pointing elsewhere per locale is content *selection*,
>   not translation.
>
>   **The overlay is row-shaped, not column-shaped.** Every other translation
>   table here is nullable columns falling back per field. A metafield value is
>   one row with one value, so the translation row's `value` is NOT NULL and "no
>   row" is the fallback. The storefront reads
>   through its own query (`listForOwnerLocalized`), never the admin's:
>   overlaying in the editor would make a translated field look canonical and the
>   next save would write it back over the original. **The metaobject overlay
>   goes inside `findPublishedFields` rather than beside it** — that read already
>   joins the value rows, and it has no admin caller to keep canonical.
> - **A metafield's `value` carries its type** (2026-08-13). `boolean` is a JSON
>   boolean and `json` is the parsed value; everything else stays a string.
>   Liquid's model — their docs say the format "depends on the type". **The
>   typing happens in the owning context's adapter**, for the same reason the
>   brand palette's role split does: `storefront` cannot see `MetafieldType`, so
>   deciding it there means matching on the literals `"boolean"` and `"json"` —
>   a second copy of an enum with nothing keeping the copies equal. **Numbers
>   stay strings**: `number_integer` normalizes through `Long` and so reaches
>   past JavaScript's exact-integer ceiling of 2^53, `number_decimal` allows 38
>   digits, and a JSON number would silently drop them — the call
>   `startingPrice` already made. Only JDK types may cross the port; a parser's
>   node type in `shared` would put a JSON library on every context.
> - **A `metaobject_reference` resolves to its entry** (2026-08-13). Liquid's own
>   rule — their docs say a reference type's `value` "directly returns the
>   referenced object", and there is no separate `reference` property. So the
>   pair stays `{type, value}` and `value` is the entry.
>
>   Fields nest under `fields` rather than sitting top-level. That is why
>   Shopify's `system` object is *not* copied: theirs exists only to keep
>   built-in names from colliding with field keys, and nesting removes the
>   collision.
>
>   A reference whose entry is unpublished, deleted or another operator's is
>   **pruned whole**, the way a dead menu link is. A bare id is worse than an
>   absent field: a theme can guard on absence and can do nothing with an id.
> - **`featuredExperiences` is top-level and capped at 12.** Shopify's globals
>   give lazy `collections`/`all_products` accessors; an eager JSON payload
>   cannot copy that, so the bound is the merchant's `featured` flag plus a hard
>   cap. The filter, order and cap all live in one derived query name, and the
>   tests parse it rather than assert a string.
> - **`linklists` is a map of menus by handle, and every link in it resolves.**
>   An item pointing at an unpublished or deleted target is dropped, and so is
>   anything nested under it. Unpublishing is how an operator takes something off
>   the storefront, so leaving the link would defeat the act.
>
>   Resolution is two batch lookups for the whole navigation, never one per item:
>   `experience` and `page` each answer for their own handles, in the rendered
>   locale.
>   The link carries `{title, type, url, levels, links}`: no `handle` (no column
>   behind it) and none of Shopify's `active`/`current` family, which are the
>   first fields whose value would depend on which address was asked for.
> - **A page route is the globals plus one object, and that object is absent
>   elsewhere.** `/pages/{handle}` adds `page`, serialized `NON_NULL`, so the home
>   page simply does not carry it. That is Liquid's model: a template gets the
>   globals plus its own object, and the others are not defined.
>
>   The page's SEO substitutes the operator's through `StorefrontGlobals.withSeo`.
>   The globals are assembled once, and the one page-shaped difference is applied
>   by the use case that knows about pages. The chain itself is `SeoText`,
>   extracted at its second caller.
> - **A handle a locale renames has one address in that locale.** The canonical
>   handle 404s there rather than serving the same page twice. It is the same rule
>   that makes `/{primary}` a 404 when the primary already lives at `/`.
> - **`shop.address` is Shopify's `address`, minus what a shop has no use for.**
>   Their `first_name`/`last_name`/`company`/`id`/`url` are customer-address
>   fields. `province_code` needs ISO 3166-2 data we do not carry. `street` is
>   theirs and derived, so it is free, and `summary` is a theme's business.
>
>   Only the **country** is a reference. ISO 3166-1 is closed at 249. Cities are
>   millions with no canonical list, and a curated table would block an operator
>   whose village is missing. `country.name` is English only; the code rides
>   alongside for a client that would rather localize it.
> - **`shop.metafields` is Shopify's shape with our vocabulary** —
>   `tourOperator.metafields.<namespace>.<key>` addressing a `{type, value}` object.
>   Their `type` codes are not ours (`single_line_text`, not
>   `single_line_text_field`), there is no `list?` because list types are out of
>   our catalogue, and a JSON consumer reads `.value` where Liquid renders the
>   metafield directly. It is assembled from a **second** shared port
>   (`StorefrontMetafieldQuery`, implemented in `metafield`), because the context
>   that owns the operator row may not read those tables.
>
> Below the quote, the rules that carry a live example say so by naming it. The
> rest is a **specification**: the shape to build to, each rule paid for once. Read
> it that way rather than assuming the code is already in that shape — it was
> deleted and rebuilt twice, and the rebuild did not follow every line of it.

A page a template renders takes **named objects, never a flat bag of scalars**,
and the same set on every page. `storefront` was the canonical one:

```
tourOperator  id, name, address, phone, email, url, description, passwordMessage,
              brand { slogan, shortDescription,
                      colors { primary [ {background, foreground} ], secondary [ … ] },
                      logo, squareLogo, favicon, coverImage,   -- Image or null
                      socialLinks [ { platform, url } ] },
              policies [ { type, title, url } ],
              cancellationPolicy, privacyPolicy, termsOfService, legalNotice,
              currency { code, symbol }, timezone { name, city }
(top level)   pageTitle, pageDescription, ogImageUrl, canonicalUrl, pageType
page          id, handle, title, body, url   -- /pages/{handle} only, NON_NULL
routes        root, experiences
localization  language, languages [ { code, name, endonymName, primary, url } ]
```

**This block is a summary of the rules above, so it has to move when they do.** It
carried `page title, description, ogImageUrl, path` and `localization locale, …
current` until 2026-08-20 — the shape from before the rules that reassigned `page`
to the CMS page and renamed the served locale. A reader reaching the block first
gets the replaced answer with nothing marking it as old.

**A named accessor beside a list is Shopify's shape and is worth copying.**
`tourOperator.policies` iterates. `tourOperator.cancellationPolicy` is the one a
booking form wants, without comparing type strings. It is **null** when the
operator has not written that policy, so a template guards on the object. The four
names are not derived from the type: `TERMS` is `termsOfService`, because that is
what a theme author coming from Shopify types.

**Anything a theme renders as an `<img>` is one shared `Image`** —
`{ url, alt, width, height, aspectRatio }`. `aspectRatio` is **derived**
(`width / height` when both are present, null otherwise) and never stored: a
third column can disagree with the two it comes from. `alt`, `width` and
`height` are **populated** since the media slice: width and height are measured
from the bytes at upload, alt is written by `PATCH .../media/{id}`. So
`aspectRatio` derives for real, and the layout-shift reason those columns exist
is finally paid for. Rows uploaded before that slice keep nulls — nothing
backfills — so a template still has to guard. An absent media reference is a **null `Image`**, not an
`Image` with a null URL, because the template guards on the object.

**There is no `tourOperator.logoUrl`.** The logo is `tourOperator.brand.logo`, where Shopify
keeps it — their shop object has no logo of its own. Removing it was a breaking
change to a published contract, made deliberately while no operator theme
existed to break (#100).

**It exists twice, in key form and URL form**, which is PATTERNS §5 applied to a
page: application deals in storage keys and locale codes, presentation resolves
both. As rebuilt that is `StorefrontGlobals` (+ `LocalizationData`, `MenuData`) in
`application/dto/output` against `StorefrontGlobalsResponse` and its nested
`Image`/`Routes`/`Localization`/`Language` records in `presentation/response`.
`routes` has no application half at all — a route is a URL — and `aspectRatio` is
derived on the same side for the same reason. **There is no `presentation/view`
package while the storefront answers JSON**; a template's context object is a
different thing from a serialized response, and that package returns with the
templates. Every record is `public` with a `public` enclosing type, nested ones
included, because the Mustache compiler runs with access coercion off.

**A collection the owning context orders is ordered by the query, and split by
the query too.** The palette is `colors.primary[0].background`, so its order is a
promise a theme indexes into. That order lives in the derived query's name,
`findByTourOperatorIdOrderByPositionAsc`, and nowhere else. It is pinned by
parsing that name with Spring Data's `PartTree` — §9's shape, from the
experiences listing.

The *role* split happens in the owning context's adapter, not the caller's. A
role is a `touroperator` enum. A flat list tagged with a role string would force
`storefront` to compare against literals, which is a second copy of an enum it is
fenced from seeing.

**One rule decides what goes in: expose what the row has, invent nothing.** A
field with no column behind it is invention and stays out; a field with a column
goes in whether or not this slice renders it.

That used to be two rules — the second demanding "a renderer in this slice or a
named caller in the next", which is what kept `tourOperator.timezone` out. It was right
for a page and wrong for a contract, and #96 dropped it: the object is API the day an
operator authors a theme, so a field added later is a breaking change while a
field added now costs one record component. `tourOperator.timezone` is in. So are
`brand.slogan` and the palette, which nothing renders yet — the shape is the
contract and the data follows. A field is omitted only when no column backs it,
or when it belongs somewhere else (theme settings, `localization`).

**A page adds its object to the globals rather than wrapping them.**
`/pages/{handle}` serves the globals plus `page`, serialized `NON_NULL`, so a
route without one simply does not carry it. A page with nothing of its own
returns the globals directly, as the home page does.

While the response is JSON this costs nothing. It starts to bite when templates
return and every page repeats the globals in a wrapper. The answer then is likely
a `Map`. Do not build it before the sections that need it.

**Where a URL that varies per page is built:** `application` says *where* a thing
lives (the locale code, and whether it is the one that serves bare), `presentation`
says *what its URL is*. **The language switcher does not do this yet, and that is a
live gap on a shipped route.** Every `Language.url` is built as `/` or `/{code}`
in `StorefrontGlobalsResponse`. So switching language from `/pages/about` sends
the visitor to the home page, not to that page in that language.

**The blocker is the data, not the plumbing.** A page's handle is translated, so
the switcher needs that page's handle *in each locale*, and no query answers that:
`StorefrontPageQuery.findByHandle` resolves one handle in one locale, and
`StorefrontExperienceQuery` is the same shape. **Do not capture the current page's
handle and repeat it under every prefix** — English prefix, Spanish slug, 404.

So it lands with the experience detail page: decide how per-locale handles arrive
(all locales on the detail query, or one call per language), then build the URL
from the resolved handle. One slice, a real caller.

**Renaming a component here is a breaking change** once operators author themes.
Decide the shape while there are four records to change, not forty templates.

## 2b. The render path (decided 2026-08-02)

**Rendering runs in Spring, in-process, with Mustache** —
`spring-boot-starter-mustache` → `spring-boot-mustache` → `com.samskivert:jmustache`
1.16, confirmed from the published POMs.

**Nothing renders a template today, and the decision is unchanged.** The storefront
was cut back to a placeholder on 2026-08-11 and answers JSON, so the dependency and
`StorefrontMustacheConfig`'s compiler sit ready with no caller. They are kept rather
than removed and reinstated because every setting on that compiler is a *finding* —
each arrived at by reverting it and watching the failure — and those stay true only
while something exercises them. **The mechanics live in `STACK.md`'s gotchas**, not
here: null and missing variables, the `DefaultCollector(false)` trade, template
inheritance, standalone section tags, and why `MustacheView` cannot be used.

**Untrusted templates are the constraint, and it is a day-one one.** Shopify built
Liquid in 2006 *because ERB executes arbitrary Ruby* — it is a sandbox, not a
convenience. Year one we author every theme; the door has to stay open for operators
authoring their own. That makes the engine a **format** decision rather than a
library one: a year of themes in a non-sandboxed language cannot be opened up later
without rewriting the corpus or running two engines forever.

**Of Spring Boot's four, only Mustache can safely run a template we did not write.**
Thymeleaf evaluates SpEL (`${T(java.lang.Runtime)…}`); Groovy is arbitrary code;
FreeMarker's own FAQ advises against untrusted authors. **Performance does not
discriminate** — every interpreted engine lands in one band (FreeMarker 14.7s,
Mustache 15.8s, Thymeleaf 18.3s per 25k renders) and render time is dwarfed by the
framework and Postgres. Only compiling engines are meaningfully faster, and they
compile to Java, which disqualifies them for the same reason.

**Liqp was the alternative and was rejected.** Liquid is nicer to author in and
has Drops. But `nl.big-o:liqp` is 178 stars and one maintainer, and a third-party
fork already exists — evidence both that the abandonment risk is real and that
vendoring is the escape.

Neither engine ships Shopify's storefront filters and tags (`money`, `asset_url`,
`{% section %}`, `{% paginate %}`). Those are ours to write in any language, so
they do not discriminate either.

**The cost we accepted, so nobody rediscovers it as a surprise.** Mustache is
logic-less: `{{price}}`, never `{{price | money}}`. Every formatting need is a Java
change or an exposed lambda, and a theme author who cannot touch Java is blocked
until one exists. Plan that helper set as real work. It is *smaller* than Liquid's
filter list, though — `Mustache.Formatter` does type-directed formatting with no
helper at all, which covers the largest family (money, dates, numbers).

**Custom tags are impossible, verified in the source.** `Mustache.java` parses tags
with a hardcoded `switch (tag.charAt(0))` over `# > < $ ^ / ! &` plus a default for
plain variables. No registry, no extension point. One consequence is forced rather
than chosen: **a section's schema cannot live inside its template** the way
Shopify's `{% schema %}` does.

**Push vs pull follows from where rendering runs.** A separate renderer has no
database, so the backend must resolve every typed reference *before* answering — a
template-JSON store, a setting-type registry, a resolver pass, and a one-call-per-page
rule to protect. Rendering in-process lets the template ask mid-render, which makes
merchant-composable sections cheap instead of a subsystem. (Shopify caps
`all_products` at 20 handles per page because they hit the same fan-out.)

**Three decisions came back the same shape after the rewrite**, which is the evidence
they were about the problem and not the renderer: tenant resolution from the Host
header rejecting the apex and multi-label subdomains (#88/#90), the strict locale
rule, and **the password gate running before the locale check** (#91). The deleted
Worker had reached all three independently.

**Still open under this decision:** the section/theme model — how a merchant composes
a page, where a section's schema lives given the constraint above, and what OS 2.0
scope we match. **Real pages force that model; it is not designed abstractly.** Also
unsettled and worth starting early: **domains**. Shopify's `{handle}.myshopify.com`
split exists for **cookie isolation**, not branding — storefronts under the admin's
domain could set cookies on the shared parent. Candidate mapping is `vointika.com`
for marketing and the admin SPA, `{handle}.myvointika.com` for storefronts, custom
domains later; getting a domain onto the Public Suffix List takes time.

## 3. Persistence per aggregate — the 6-file recipe

For an aggregate `Foo`:

1. `domain/entity/Foo` — pure domain entity.
2. `domain/repository/FooRepository` — the interface (what the use case sees).
3. `infrastructure/persistence/entity/FooJpaEntity` —
   `@Entity @Table(schema="<ctx>", name="foos")`, Lombok
   `@Getter @NoArgsConstructor @AllArgsConstructor`.
4. `infrastructure/persistence/mapper/FooMapper` — static `toDomain(jpa)`
   (+ `toJpa` when there are writes).
5. `infrastructure/persistence/repository/FooJpaRepository` — Spring Data
   `extends JpaRepository<FooJpaEntity, UUID>`.
6. `infrastructure/persistence/repository/FooRepositoryImpl` —
   `@Repository implements FooRepository`, delegates to the JpaRepository + Mapper.

Canonical: identity `User*`, reference `Timezone*`.

## 4. Reference-data slice (read-only lookup)

A curated, read-mostly table (countries, timezones, currencies). = the
persistence recipe (§3) **plus**:

- `application/usecase/ListFoosUseCase` — returns `repository.findAll()`.
- `presentation/controller/FooController` — `GET /api/foos`, **authenticated**
  (no public route), maps domain → `FooResponse`.
- `presentation/response/FooResponse` — a record following the response identity
  convention below (§4a).
- `db/migration/<ctx>/V?__*.sql` — seeds the curated launch set.

**There is no nested-only variant any more.** It used to say that a reference type
used only inside another response — `Country`, nested in a timezone — keeps entity,
JpaEntity, Mapper and Response, and drops the repository, use case and controller.
`Country` was its only example. The structured-address slice then needed a
repository to validate a country id and an endpoint to populate a picker, so the
variant has zero live instances and the paragraph went with it. The underlying
instinct survives in LAW §2.4: don't add the endpoint until something needs it,
and `GET /api/countries` was in fact deleted once for exactly that reason before
coming back when a caller appeared.

Canonical: `reference` — `Timezone`, `Currency`, `Language` and `Country`, all
full slices.

## 4a. Response identity — `id` + `context` (HOUSE RULE)

Every resource-representing response record identifies itself with exactly two
meta fields:

- **`id`** — the entity's id. **Never** a prefixed name (`userId`, `operatorId`,
  `tourOperatorId`, …). Just `id`, always.
- **`context`** — a string naming the collection the entity belongs to
  (`"users"`, `"currencies"`, `"timezones"`). Set via the two-constructor pattern
  so callers never pass it. (This is the discriminator; it is **not** called `type`.)

The `context` is the entity's *own* collection: a team-member row is a user with a
role, so it is `id` = the user's id + `context: "users"` (not `"members"`).
Action-result responses that aren't a resource (e.g. `LoginUserResponse`,
`SetAvatarResponse`) carry neither field.

Canonical: `CurrencyResponse` (`id`, `context:"currencies"`), `MemberResponse`
(`id`, `context:"users"`).

## 4b. Paginated list endpoints (cursor + filter + sort)

Any list that **grows with business activity** — members, bookings, orders, audit —
MUST use the shared list framework. **Never an unbounded array.** The roster
shipped that way once, and this recipe is what came of fixing it.

**Tenant-scoped is not the test, and this used to read "tenant *or* growable"** — a
disjunction that twelve shipped endpoints contradict. Every `/translations` read
returns a bare `List<>` and is right to: it is capped by the operator's enabled
locales, a closed set of six, and a cursor over six rows is machinery for nothing.
The question is whether the collection has a ceiling the operator cannot raise by
using the product. **`/metafields` is the honest edge** — bounded only by how many
definitions the operator creates, so it is on the array side by assumption rather
than by argument, and is the first thing to move if an operator ever defines enough
of them to notice.

1. **Schema** — a `public static final ListSchema SCHEMA` on the use case:
   `.tenantScoped()` (scopes to the entity's `tourOperatorId`), `.set/text/number/
   instant(...)` for each filterable field, `.sortable(...)` + `.defaultSort(...)`.

   **A `.sortable(...)` field must map to a `NOT NULL` column.** The cursor is a
   keyset on that column. In SQL a `NULL` comparison is *unknown* rather than
   false, so a row with a null sort value matches neither the `>`/`<` nor the `id`
   tie-break. It **disappears after page one** — no error, no log line, just a
   list that is quietly short.

   **A nullable column may be filtered, but only with a positive operator, and
   the schema has to say it is nullable.** The same three-valued logic bites one
   step along: `neq`, `not_contains` and `not_in` are built as a negation, and
   `NOT (NULL LIKE 'x')` is unknown, so every row with no value is dropped from a
   result it belongs in. Measured before the rule existed — 12 contact messages,
   one nameless, and `not_contains=zzz` returned 11: a filter that excludes
   nothing still lost a row. Mark the field `.nullable("actorName")` and
   `ListQueryParser` answers **422** for those three operators rather than a short
   list; `contains`, `eq` and `starts_with` keep working, which is why this is a
   per-operator refusal and not a ban.

   *Refuse rather than repair, by decision.* `cb.or(cb.isNull(path), …)` would
   also work, but it makes `neq` stop being the complement of `eq` and picks a
   semantic on the caller's behalf. A 422 is honest about a limit and can become a
   repair later without breaking anyone.

   *Which columns can stay nullable.* Make it `NOT NULL` where the value is really
   required — `contact_messages.name` was nullable by mistake and is not any more
   (contact/V3). Where a null is the truth, keep it and declare it:
   `audit_log.actor_id`/`actor_name` are null for a `SYSTEM` entry, which has no
   user behind it to name.

   Two guards, both deriving the endpoint→entity pairing from the
   `listExecutor.list(...)` call so a new list endpoint is covered the day it is
   written: `SortableColumnsAreNeverNullableTest` (sorting is forbidden outright)
   and `FilterableNullableColumnsAreDeclaredTest` (the declaration must match the
   entity **both** ways — an undeclared nullable column is the live bug, a
   declared `NOT NULL` one needlessly 422s a working filter). They share
   `ListSchemaScanner`; fixing a blind spot in one must not leave the other blind.
2. **Repository** — `CursorPage<Foo> list(ListQuery query)`, delegating to the
   shared `CriteriaListExecutor.list(FooJpaEntity.class, SCHEMA, query, Mapper::toDomain)`.
   The executor does keyset cursor pagination (page size 20, tie-broken on `id`),
   the filter predicates, and the sort. **The page size is the framework's, not
   the caller's** — one constant for every list in the application, by decision,
   and there is no parameter to override it. Never introduce a per-resource one.
3. **Use case** — `execute(ListQuery, callerId)`: gate (e.g. `ensureMember`), call
   `repository.list`, enrich the page's rows (batched, no N+1), return the
   `CursorPage` with its `nextCursor` unchanged.
4. **Controller** — inject `ListQueryParser`, `parse(request, SCHEMA, tenantId)` →
   `ListQuery`, return `CursorPageResponse.of(page, FooResponse::from)` →
   `{ "data": [...], "nextCursor": "..." }`.

Query shape: `?filter[role][in]=OWNER,ADMIN&sort=-joinedAt&cursor=…`. The cursor is
opaque (base64, keyset on sort-field + id); `nextCursor` is null on the last page.
**Those three names are the whole accepted set** — anything else is a 422, so a
list never silently ignores part of a request (#134).
Canonical: `ListMembersUseCase` + `GET /api/tour-operators/{id}/members`.

## 4c. One DTO or two at the application boundary

A use case takes an `Input` from `application/dto/input` and the controller owns a
`Request` in `presentation/request`. Keep both **only when they differ**. In identity
nine pairs do — the `Input` carries a `userId` from the authenticated principal, or a
`language` the body never had — and four were byte-identical copies, since deleted.

An identical copy is not a seam. Add a field to the request that the use case needs
and both change in lockstep, so it insulates nothing while costing a file and a
mapping step.

**Check the nested records separately from the wrapper.** `ReplaceMenuItemsRequest`
and `ReplaceMenuItemsInput` genuinely differ: the input adds the caller and the two
path ids. But the tree node inside them was identical, so the controller carried a
recursive copy that ran on every save.

A pair can be a real seam at the top and a pure copy one level down. The nested
type is where the cost is, because collapsing it deletes a mapper and not just a
file.

When you collapse one, **the application record is the survivor** and the controller
binds to it:

```java
public ResponseEntity<LoginUserResponse> login(@RequestBody LoginUserInput input) {
    var output = loginUserUseCase.execute(input);
```

Never the other way. A use case referencing a `presentation` type inverts the layer
graph and ArchUnit fails the build.

There is one condition, and the build enforces it: **the surviving record carries
no annotations.** The application layer's allowlist is `com.vointika..` plus
`java..`, so a `@JsonProperty` or a Jakarta validation annotation on it compiles
and then breaks the build. When that happens the answer is to reintroduce a
presentation DTO — the shapes have genuinely diverged.

Responses are the mirror image: `LoginUserOutput` carries `accessToken` *and*
`refreshToken`, `LoginUserResponse` carries only the access token because the refresh
token leaves in an httpOnly cookie. That pair stays.

**An *identical* output pair is still undecided, and this rule does not reach it.**
`OperatorLocalesView`/`OperatorLocalesResponse` and
`StorefrontPasswordView`/`StorefrontPasswordResponse` are field-for-field identical
with copy-only mapping. The rule above collapses identical pairs on the **input**
side, and keeps the one output pair it names because those two genuinely *differ*.

The asymmetry is why it stayed open. Collapsing an input pair means the controller
binds its body to the application record. Collapsing an output pair means **the
application record becomes the serialized wire contract** — so renaming a field
inside the application layer is an API break, and the usual escape hatch is barred,
because the allowlist forbids putting `@JsonProperty` on it to decouple them.
Decide it, then either collapse both or write the exemption here. Until then, do
not collapse an identical output pair on the strength of the input rule.

## 4d. Two namespaces read as one must be validated as one

A storefront handle resolves against **localized handles first, canonical handles
second**. That makes them one namespace on the read side. So uniqueness has to be
checked across both on every write. Otherwise one silently shadows the other, and
the shadowed page becomes unreachable in that locale with no error at any point.

> **The read half was deleted twice and is back.** The `rendering` context and its
> four `Storefront*Query` seams went on 2026-08-02; the in-process rebuild's
> shop and experience queries went on 2026-08-11 with the placeholder cutback.
> Both handle-resolving paths were rebuilt afterwards: `TenantHandleResolver` +
> `GetStorefrontGlobalsUseCase` resolve a *tenant* handle, and
> `StorefrontPageQueryImpl.findByHandle` resolves a *page* handle. The rule below
> is what they honour — it is no longer advice for a future implementer.
> **The write guards below were kept anyway.** They cost nothing. Dropping them
> would let a shadowing handle be stored while nothing can observe it. It would
> then surface as an unreachable page the day a read path returns: a defect
> committed now and discovered much later.

`page` shipped with each namespace checked only against itself, which is the natural
mistake: the create/rename path asks `pages`, the translation path asks
`page_translations`, and each looks complete on its own. The three write paths now
cross-check:

- **create or rename a canonical handle** → also reject it if any *other* page uses
  it as a localized handle in **any** locale;
- **upsert an explicit localized handle** → also reject another page's canonical handle;
- **derive a localized handle** → probe *both* namespaces, so the auto-suffix never
  lands on one either.

Matching the page's **own** canonical handle is fine — it resolves to the same page.
The general rule: when a read path consults two sources in precedence order, list the
write paths that feed each and make every one of them check both.

`experience` had the same defect and now has the same guards. One difference is
worth knowing before you read the two side by side and think one is wrong.

**Whether a cross-namespace collision is a 409 or a suffix depends on who chose the
value, not on which namespace it came from.** A page handle is operator-chosen and
permanent, so a clash is a 409 the operator can act on. An experience's canonical
handle is *derived from its name*, so its create path widens the probe instead: the
auto-suffix steps over localized handles too. The operator sees a `-2` rather than a
409 for a value they never typed and have no field to correct. The explicit
localized handle is operator-chosen in both, and 409s in both.

One consequence: the any-locale probe needs an exclusion parameter only where a *rename*
path calls it (page). Where the canonical value is immutable (experience), create is the
only caller and never excludes — so the parameter, and page's nil-UUID sentinel standing
in for "exclude nothing", are both absent by LAW §2.4.

**These guards are pre-checks, not constraints, and that is the one thing this
recipe cannot fix.** Uniqueness *within* a namespace is backed by a unique index, so
a lost race surfaces as a duplicate-key failure and the loser is rejected.

No index spans the two tables, and none can without a trigger. So two concurrent
writes, one per namespace, can still land on the same value and produce exactly the
shadowing the guards exist to prevent. The window is small, and both `page` and
`experience` carry it. Treat the cross-namespace check as closing the
reachable-by-one-request hole, not as making the invariant true.

## 4e. The translation-overlay table (eight of them, in two shapes)

A translatable aggregate gets a sibling table keyed on
`(<owner keys…>, locale)`, and the read overlays it
**nullable-wins-canonical**: a null column falls back to the owner's own value,
never to an empty string. A row overlays; it does not replace. Translating a
title and not a body is a Spanish title over an English body, which is the
realistic partial-translation case and the one fallback bugs hide in.

**Nine tables do this**, in **two shapes**. Seven are *column-shaped* — nullable
columns falling back per field:

| | owner key | content columns | overlaid by | clearing |
|---|---|---|---|---|
| `experience_translations` | single id | 6, nullable | `StorefrontExperienceQueryImpl` | blank → null |
| `tour_operator_translations` | single id (the operator) | 5, nullable | `StorefrontTourOperatorQueryImpl` | blank → null |
| `tour_operator_policy_translations` | **composite** `(operator, type)` | 2, nullable | `StorefrontTourOperatorQueryImpl` | blank → null |
| `page_translations` | single id | 5, nullable | `StorefrontPageQueryImpl` | blank → null |
| `menu_item_translations` | single id | 1, **NOT NULL** | `StorefrontMenuQueryImpl` | **blank → 422** |
| `audience_translations` | single id | 1, nullable | **nothing** | blank → **delete** |
| `category_translations` | single id | 1, nullable | **nothing** | blank → **delete** |

Two are *row-shaped*, added by the metafield translation slices (#151, #152):

| | owner key | content | overlaid by | clearing |
|---|---|---|---|---|
| `metafield_value_translations` | **composite** `(value, locale)` | one row, **NOT NULL** | `MetafieldValueJpaRepository` (`COALESCE`) | **delete the row** |
| `metaobject_entry_value_translations` | **composite** `(entry value, locale)` | one row, **NOT NULL** | `MetaobjectEntryJpaRepository` (`COALESCE`) | **delete the row** |

**The row shape is why the split is not simply "column count".** A metafield value
*is* one value, so there is no nullable column to fall back per field. "No row" is
the fallback instead, which is why `value` is `NOT NULL` and clearing is a
`DELETE`. It also overlays in **JPQL** — `COALESCE(t.value, v.value)` over a
`LEFT JOIN` — rather than in Java, because the fallback is per row and the query
can express it.

**Two are never resolved to a locale** — `audience_translations` and
`category_translations` — because neither is on the storefront yet: full admin
CRUD, no reader. That is what "the write half shipped first" looks like, twice.
A category is the newer of the two and the more likely to gain a reader, since a
storefront category page is what would give it a URL. The admin deliberately returns *every* locale
rather than a resolved one — `GetMenuUseCase` hands back a locale→title map —
which is what an editor needs.

Same one level down: only **3 of experience's 6** translatable columns are read
(`handle`, `name`, `description`), because the listing card is the only consumer.
The rest are stored and never rendered in any locale — and that gap is what
experience/V11 and V12 acted on, dropping `highlights`, `included` and
`not_included` from the owner and its overlay together once it was clear no
reader was coming. It was 9 until then.

**Two shapes, and the split is the number of content columns.** The nullable rule
exists so a partial translation can be expressed. With one column there is no
partial state — a row without its only value is not a partial translation, it is
an empty row. So:

- **Multi-column** — every content column nullable, blank clears the field.
- **Single-column** — the row either exists or it does not. `menu_item_translations`
  makes its column `NOT NULL` and answers **422** to a blank; that is *more*
  correct for its shape, not a violation to tidy away.

**An overlay with nothing left in it is deleted, not stored.** It falls back for
every field, so it is indistinguishable from having no row — except in the
translations list, where it shows as a locale someone worked on. Every upsert
asks the record's own `isEmpty()` and deletes instead of writing, auditing
`*.translation_deleted` because that is what happened; saving an already-blank
form writes nothing and logs nothing. `isEmpty()` is the mirror of the `empty()`
factory each record already had for the editor's blank form, and each record's
test pins `empty(...).isEmpty()` so a new translatable field that misses
`isEmpty()` fails.

**`menu_item_translations` is the one deliberate outlier, and it is not lazily
written.** Its items are not editable individually: the whole tree is POSTed and
rebuilt with fresh ids. So translations ride inline in that payload, have no
endpoints of their own, and are cleared by being left out.

**Three of the nine carry no `tour_operator_id`, and all three for the same
reason: their parent has none either.** `menu_item_translations` hangs off
`menu_items`, which is reached through its `menus` row and takes the tenant from
there. The two row-shaped metafield overlays hang off `metafield_values` /
`metaobject_entry_values`, which are **owner-generic by design** — a bare
`owner_id` with no FK, so there is no tenant column to inherit (the *definitions*
are tenant-scoped; the values are not). Adding one to any of the three would be a
migration for a join nobody needs.

*This said "the only one" until 2026-08-20.* It was true when written and stopped
being true when #151/#152 added the two metafield overlays — and it was then
**restated** in a later edit that did not recheck it, which is the failure worth
noticing: a sentence can be reworded long after it goes false.

**Still not generalising, and the arithmetic is now the reason rather than the
excuse.** Five of the seven column-shaped tables have a reader — the audience and
category overlays have none — and they resolve through **four** adapters, because
`tour_operator_translations` and `tour_operator_policy_translations` share one.
Each adapter carries the same byte-identical helper:

```java
private static String overlay(String translated, String canonical) {
    return translated != null ? translated : canonical;
}
```

**Four copies of it exist** — `StorefrontExperienceQueryImpl`,
`StorefrontTourOperatorQueryImpl`, `StorefrontPageQueryImpl`,
`StorefrontMenuQueryImpl` — called at 16 sites. Sharing it means pushing two
lines into `shared` and coupling four contexts to it, to remove six (LAW §2.4).
The row-shaped two cannot use it at all: their overlay is `COALESCE` in the
query. Keep the **rule** identical, not the code.

Revisit if the helper ever grows a second statement, or if audiences gain a
storefront reader and the count moves again. A fifth byte-identical copy is still
cheaper than the coupling; a copy that starts *differing* is the signal.

## 5. Read-time URL resolution (never store URLs)

Store a bucket-relative **storage key** on the row; resolve it to an absolute
URL **at read time** in the controller via `shared.media.MediaUrlResolver`. The
response exposes `<thing>Url`. Changing the bucket/domain then needs no data
migration.

- Key shapes: `users/{userId}/{uuid}-avatar.{ext}`, `flags/{iso2}.svg`.
- Canonical: identity avatar, reference country flag.

## 6. Cross-context communication

A context never imports another's types. Two channels only:

- **Shared query port** (synchronous read) — `shared.port.<Noun>Query` +
  a `<Noun>View` record. The owning context implements it in
  `infrastructure/query`; the caller depends on the interface. Views carry
  primitives, not another context's enums.
  Canonical: `UserAccountQuery`, `UserTourOperatorMembershipsQuery`.
- **Event over Kafka** (asynchronous) — see §7.

**A rule two contexts state identically belongs in the kernel, not in either one.**
Not a third channel — `shared` is importable by everyone, so this is about where a
type lives, not about contexts talking. Four have moved so far: `HandleGenerator`,
`OperatorLocaleCheck` (seven call sites in five contexts), and
`shared.valueobject.SeoTitle`/`SeoDescription`, which replaced six per-context
records — `experience`'s pair and `touroperator`'s were byte-identical apart from
javadoc, and `page`'s differed only by prefixing `"Page "` onto each refusal.

Two things to settle before moving one, in order:

1. **Is a published contract involved?** Run the sentinel probe in §9a *first*. The
   SEO collapse grepped to zero snippets, so it was a refactor; had `page`'s prefixed
   wording been a published example, the same edit would have been a breaking change
   dressed as cleanup.
2. **Does the sameness carry weight, or is it coincidence?** Three contexts capping
   an SEO title at 70 is one fact (SERP truncation) written three times. Three
   contexts capping a *name* at 200 might just be three independent choices that
   happen to agree, and merging those couples them forever. Ask what would have to be
   true for one to change alone.

The counterweight is §2.4: a constant with one caller is not a kernel type. Two
independent statements of the same rule is the threshold.

**Two questions decide it, and they are not the same question: does it have a home, and
does it have an owner?** The ADMIN 403 sat un-collapsed through six passes because each
one asked only the first. Fifteen test files across **seven** contexts spelled out
`"This action requires ADMIN privileges"`, eight of them publishing it, while production
*interpolated* it — so there was no constant to reference, and every pass could truthfully
say the other fourteen files were not its to fix. **A duplication that spans more contexts
than any pass covers has no owner, and deferring it is deciding never to do it.** Its home
was obvious once looked for: the shared port that already holds `TENANT_NOT_FOUND`.

The mirror case is the one to leave alone. `"File is empty"` is an identical guard in
`media` and `identity`, and it stays duplicated: two callers, and the only shared home
would be a new class holding one `if`. `"Content type is required"` looks like the same
find and is not — media's is a **domain value object's invariant**, identity's a **use-case
precondition**, so they are different statements that share a sentence. Ask which layer
each lives in before calling two copies one fact.

**And a published message is collapsed only when one assertion still holds its wording
— see §9a.** This is the half that gets skipped, because the collapse feels finished when
the build goes green. It is not: the copies you removed were failing assertions, and
replacing all of them with calls to the new helper makes every one of them hold for any
value. The ADMIN 403 shipped that way in the same PR that wrote this paragraph — reword
the helper and **eight** published bodies change with a green suite, which is strictly
weaker than the fifteen literals it replaced.

**Pin it against whatever the message is built from, not against a copy of its output.**
`TENANT_NOT_FOUND` is a constant, so a literal pin is the whole story. A message built
from an argument has a second joint: production passes `minimum.name()` while every stub
hardcodes `"ADMIN"`, so renaming the enum constant reworders production while all fifteen
stubs keep agreeing with themselves. `theInsufficientRoleMessageReadsThisWay` asserts the
literal against `MemberRole.ADMIN.name()` and fails on **both** the reword and the rename;
a pin written against `"ADMIN"` catches only the first.

**Two copies of one rule can pin opposite behaviour, with both suites green.**
`identity.Email` and `touroperator.InviteeEmail` stated the same rule; identity's test
asserted `"  user@example.com  "` is *rejected*, touroperator's asserted it is
*accepted and trimmed*. Neither was wrong about its own record, so nothing failed —
and the same human input succeeded on the invitation path and 422'd on registration.

So when you merge N copies, **diff their tests, not just their code**. Identical
implementations can still have contradictory expectations attached, and the merge
forces a choice that a reader of either file alone would not know was open. Record
which way it went and why in the surviving test, because that test now silently
contradicts a deleted one.

## 7. Event flow (Kafka)

- **Publish:** a use case calls `EventPublisherPort.publish(new FooEvent(...))`.
  Event records live in `shared/event`. The producer adapter routes by class →
  topic (`shared.infrastructure.kafka`); topic name = `<producing-context>.<event-kebab>`,
  registered in `EventTopics`, keyed by recipient (e.g. email).
- **Consume:** `@KafkaListener` in the consuming context's
  `infrastructure/consumer`; fire-and-forget consumers log-and-swallow so one bad
  record never stalls the partition.
- **Request context rides the event/headers:** the consumer can't query the
  producer's DB, so carry what it needs on the event (e.g. `locale`) or a Kafka
  header (correlation id).
- Canonical: identity events → notification consumers.

> **Fire-and-forget is non-critical-only.** This shape suits *drop-tolerant*
> notifications — recoverable by the user, and the SES adapter already retries
> transient failures. A **must-not-drop** event (payment captured, refund issued,
> booking cancelled, order placed) must **not** log-and-swallow. **Build the shape
> below when the first such event lands, not before (§2.3)** — it is recorded here
> so it is not improvised then.

### 7a. Critical-event delivery — decided, unbuilt

The direction only. The exact Spring Kafka 4 mechanism (`DefaultErrorHandler` +
`DeadLetterPublishingRecoverer`, ack modes, outbox) is **verified against the
pinned docs at build time** (LAW §4), never from this list.

- **At-least-once** — the consumer commits the offset only *after* the side effect
  succeeds (manual ack), never before.
- **Idempotent consumer** — redelivery is expected, so dedupe: an event-id key
  checked and inserted in the same tx as the effect, or a naturally idempotent
  domain operation (refund-by-id). No double charge or refund on replay.
- **Retry + DLQ** — transient failures retry with backoff, then land on
  `<topic>.DLT` for inspection and replay. Never swallowed, never blocking the
  partition — the opposite of fire-and-forget.
- **Key by aggregate id**, not by recipient, so per-aggregate events stay ordered.
- **Durable publish** — *open sub-decision*: transactional outbox (publish in the
  DB tx, relay to Kafka) versus `acks=all` and tolerating the dual-write window.
  Decide when the first critical producer lands.

**A sweep rides with this slice.** Configuring a container-level error handler lets
the *fire-and-forget* factory own log-and-swallow too, which collapses the **six
identical try/catch blocks in `notification`'s consumers** — one per email, each
catching `Exception`, logging and carrying on so a bad record cannot block the
partition. They are duplicated deliberately: changing how the app behaves on
failure needs the pinned docs and a running broker, so it belongs here rather than
to a subtraction pass. **None of those six consumers has a test**, so whatever
replaces the try/catch is the first thing to verify their wiring at all.

**Trigger:** the first payment, sales or booking-state event.

## 8. Config-driven capability (grow by config, not code)

A capability whose *set* grows over time (UI languages, email locales) is a
config allowlist (`@ConfigurationProperties`), validated in the use case, and
exposed via a read endpoint when the frontend needs the list. Growing it = add a
config key (+ any assets like a template file); no migration, and no code
*for the capability itself*.
Canonical: `app.identity.ui-languages` + `GET /api/ui-languages`.

**Count the consumers of the allowlist before calling it config-only.** Adding a
UI language is a yml edit for the picker and for validation. But transactional
email keeps its own list, `ClasspathTemplateCatalog.LOCALES`, plus a template pair
per (type, locale). A language missing from that list does not fail: the send falls
back to English, so the user silently gets the wrong language.

A second list that must agree with the allowlist needs a test that fails the build
when they diverge — `TemplateLocalesTrackUiLanguagesTest` — not a comment saying it
should track.

## 8a. Rate limiting (three layers)

One primitive — `RateLimiterPort.tryAcquire(key, limit, window)` (fixed-window,
**fail-open**, Redis-backed). Keys are `rl:{dimension}:…`; callers own their
namespace. Pick the layer by what you're defending:

- **A — per-IP endpoint rules** (pre-handler, `EndpointRateLimitFilter`): abuse
  defense on public/unauthenticated routes. Declare per-context via the
  `RateLimitRuleRegistrar` SPI (mirrors `PublicRouteRegistrar`) — a
  `RateLimitRule(method, pathPattern, limit, window)`. Matched by Spring
  `PathPattern`, keyed **`rl:ip:{METHOD}:{pattern}:{ip}`**. Canonical:
  `IdentityRateLimitRoutes`, `TourOperatorRateLimitRoutes`.
- **B — per-identity in-use-case throttles**: input-dependent keys the filter
  can't see (email, account). Inject `RateLimiterPort`, call
  `tryAcquire("rl:{action}:{scope}:{value}", n, window)`. Canonical:
  `RegisterUserUseCase` (`rl:register:email:{email}`).
- **C — blanket per-user cap** (`ApiRateLimitFilter`, per authenticated user): a
  runaway-script backstop, automatic on the authed API. Nothing to add.

**The keying gotcha:** layer A keys on the matched **pattern**, never the concrete
URI — else a path-variable route (`/api/invitations/*/accept`) buckets per token
value and never limits. Rules live with the context that owns the endpoint (SPI),
never in a central hardcoded map.

## 8b. Audit append (every operator-facing mutation)

A use case that mutates an operator-facing entity records the action through
`shared.port.AuditTrailPort.append(NewAuditEntry)`, **inside the same
`TransactionRunner` block as the mutation**. The entry then commits and rolls back
atomically with the action, so there is no unaudited mutation and a failed append
fails the action.

One exception: a mutation whose target is object storage (S3) appends in its own
transaction AFTER the successful write. Storage cannot roll back, so that is the
honest best-effort.

Actor name is frozen at write. It is filter-only and never sortable, because it is
nullable and keyset cursors need non-null sort keys.
Canonical: any experience/audience mutating use case; the port impl lives in
`audit/infrastructure/integration`.

## 8d. Losing a race against a unique constraint

A write that passes its `existsBy…` pre-check can still lose to a concurrent one:
the DB constraint decides. JPA flushes at **commit**, so the failure surfaces from
`transactionRunner.run(...)`, never from `repository.save(...)` — translating in a
repository implementation would miss it.

`SpringTransactionRunner` is the one translator. It converts Spring's
`DuplicateKeyException` into `shared.exception.UniqueConstraintViolationException`;
a use case catches that and answers in its own terms:

```java
try {
    transactionRunner.run(() -> { repository.save(x); ... });
} catch (UniqueConstraintViolationException e) {
    throw new ResourceAlreadyExistsException("A page with that handle already exists");
}
```

Two things this deliberately does **not** do. It does not translate the parent
`DataIntegrityViolationException` — foreign-key, not-null and check-constraint
failures are defects, not races, and must stay 500s. And it does not let the
framework type reach the application layer; ArchUnit fails the build if it does.

**So a use case that must answer for a NON-unique constraint has to ask, not catch.**
Postgres raises 23505 for a unique violation (→ `DuplicateKeyException`, translated)
but 23503/23502/23514 for foreign-key/not-null/check (→ the untranslated parent).
`DeleteMetaobjectDefinitionUseCase` caught `UniqueConstraintViolationException` to
turn "a reference metafield still pins this type" into a 409; the catch could never
fire and the delete 500'd. The fix is an `existsBy…` pre-check before the write —
which leaves the concurrent-creation race as a 500, and that is the intended outcome
for an unexpected constraint failure. `SpringTransactionRunnerTranslationTest` pins
the boundary.

Uncaught, the exception maps to 409.

The same move works for any library a use case reaches for: `MetafieldValueValidator`
needed a JSON parser, so it asks `JsonSyntaxPort.isWellFormed(value)` and the Jackson
call lives in `infrastructure/port`. The application layer has no exemptions — a
library in a use case means a port is missing.

**`javax.*` is not `java.*`, and the allowlist means it literally.** The rule permits
`com.vointika..` and `java..`; `javax.crypto.Mac` matches neither, so HMAC in a use case
fails the build exactly like a third-party jar would. The storefront's unlock cookie was
written as an `application/policy` class on the assumption that "pure JDK" was enough —
`UnlockTokenPort` + `HmacUnlockToken` in `infrastructure/security` is what it became.
Both are live: the gate returned in #138, so this is a description and not only a
lesson.
Reading the rule says this; only running it proves it, which is the point.

**Logging follows the same rule.** Some side effects fail and the caller does not
care: deleting an object whose row is already gone, or enqueuing a welcome email.
The adapter swallows and logs those, and the port documents that it never throws.

The use case reaches for `DiagnosticLogPort` only when it has something of its own
to report: a security signal, or a branch taken because config was missing. That
port takes the calling class, so log names still point at the reporter.

## 9. Testing shapes

- **Unit** — JUnit5 + Mockito, no Spring: every value object, entity behavior,
  and use case (inline `TransactionRunner` lambda for the tx). Carry the
  security-critical cases (anti-enumeration, timing parity, token rotation).
- **Controller documentation test** — `@WebMvcTest(FooController.class)` +
  `@Import(SecurityConfig.class)` (add `IdentityPublicRoutes` when testing a
  public route), collaborators `@MockitoBean`, assertions + RestDocs
  `document(...)`. Authenticated endpoints send `Authorization: Bearer …` and
  stub `AccessTokenValidatorPort.isValid/extractUserId`.

  **Never `.with(csrf())`.** `SecurityConfig` disables CSRF, so it changes no
  behaviour. But whatever the request carries is *published*: MockMvc's token
  lands in the guide as a `_csrf` query parameter that the API does not accept,
  or, on a `DELETE`, as an invented form body. It reached 52 of 153 operations
  before anyone diffed the generated output. `DocumentationTestsPublishNoCsrfTest`
  now fails the build on it.

  It is an instance of a general rule: **a documentation test's request is a
  published example, so anything added to make the test pass is added to the
  contract.**
- **A `default` method on a mocked interface is never executed.** Mockito stubs a
  default like any other method, so its body does not run in any test that mocks the
  interface — and every repository here is mocked in every use-case test. Collapsing
  twenty inline `orElseThrow`s into `MetafieldDefinitionRepository.requireByIdentity`
  moved that throw somewhere no existing test could reach: the assertions that used to
  exercise it had been rewritten into stubs of the method that now contains it.

  Two of the five `require*` methods ended up executed by nothing at all, and the
  suite was green. Caught in review by mutation — `orElseThrow(...)` → `orElse(null)`,
  still green — where production would have NPE'd into a 500 on the path that
  publishes a 404.

  The fix is a test that mocks the interface, stubs the **abstract** method, and calls
  `doCallRealMethod()` on the default: `TenantScopedLookupTest`. **Mutate the default's
  body and watch it fail**, or the guard is proving only that the mock returns what it
  was told.

  **And do the same at the call sites, rather than stubbing the default there.** When
  `experience` collapsed twelve `orElseThrow`s, the sixteen existing use-case tests broke
  loudly — the mocked default returned null and they NPE'd — and the tempting repair is
  to stub `requireByIdAndTourOperatorId` directly. That turns every `unknownExperienceIs404`
  into a tautology: the test tells the mock to throw, then asserts it threw. Adding one
  `doCallRealMethod(...)` line in each `setUp` instead keeps the abstract stub as the
  input, so the real branch runs. The mutation proves the difference — inverting both
  defaults failed **7** tests, 5 of them the pre-existing call-site 404s that would
  otherwise have gone quiet.

  **The pair to add is `requireX` *and* `requireExists`.** Three contexts have now
  landed the same second finding: the endpoints that only need the row to *exist* were
  calling `findByIdAndTourOperatorId(...).isEmpty()`, reading a whole aggregate — an
  experience with its media list, a page with its body — to answer a boolean. It is
  four sites in `experience` and four in `page`, always the translation endpoints,
  because they work entirely off the overlay table afterwards. When you collapse the
  `orElseThrow`s, check which callers use the row they just loaded.

  **The repo meets this everywhere as of 2026-08-20** — the last 47 call-site stubs, in
  `touroperator` (24) and `metafield` (23), were converted in one change. Before that they
  stubbed the default directly: `BrandUseCasesTest` told `requireById` to throw and then
  asserted it threw.

  **What the conversion bought, measured both ways — and the deltas are the claim, not
  the absolutes.** Breaking the defaults' *empty* branch: **35 → 42** failing tests, i.e.
  **+7**, exactly the sites that asserted a 404 and were therefore tautological. Breaking
  the defaults' *body* outright: **+15** test classes now notice, which is what the other
  40 happy-path stubs bought — they had bypassed the default entirely, so its body could
  have been anything.

  The absolute figures on that second row depend on how many defaults the mutation guts:
  22 → 37 over the twelve value-returning ones, 25 → 40 if the three `void`
  `requireExists` guards go too. **Same +15 either way** — which is why the delta is the
  number to quote, and a reminder that a count without its scope is half a fact (§9a).

  That split is worth knowing before doing this elsewhere: converting a `thenThrow` stub
  fixes a tautology, converting a `thenReturn` one closes a blind spot. Neither is
  visible from the diff.

  Count before believing any number, including these. A stub is `when(mock.requireX(…))`
  or `doThrow(…).when(mock).requireX(…)`; `doCallRealMethod().when(mock).requireX(…)` is
  the *fix* and must not be counted, which is how the same question got answered 32, 47
  and 54 in one review round.

- **The read-only column guard** — a table whose columns are mapped
  `insertable/updatable = false` gets a test asserting a column is writable
  **exactly while the domain can carry it**, as a biconditional. Three tables
  have one: `BrandColumnsStayReadOnlyTest`, `PolicyColumnsStayReadOnlyTest`,
  `ContactColumnsStayReadOnlyTest`.

  It exists because the hazard runs **both ways** and neither direction fails
  loudly:
  - *writable with no domain field* — the mapper has nothing to put in the
    column, so every unrelated save writes a null over it. This is not
    hypothetical: before the brand write path, setting a logo issued an UPDATE
    that would have nulled the slogan.
  - *read-only with a domain field* — the write lands in the use case, every
    test that stops at the repository mock passes, and Hibernate silently omits
    the column from the UPDATE.

  **Write it as a biconditional, not a one-way assertion.** Two of these began as
  "everything is read-only, because nothing writes it" and had to **invert** when
  the write path landed. The biconditional form inverts by itself: the slice that
  adds the writer flips the test by adding the domain accessor, not by editing the
  test.

  A one-way `isFalse()` has to be rewritten instead. A test you rewrite alongside
  the change it guards has guarded nothing.

  Exclude the keys and `createdAt`: a primary key is written by definition, and
  `createdAt`'s immutability is its own decision rather than a statement about
  the domain. Mutation-check it in the direction the slice moves.

- **ArchUnit** — **do not add a per-context isolation rule.** This line used to say
  to add one; it was stale. `contexts_do_not_depend_on_each_other` derives the
  slices from the package structure, so a context is fenced the day its package
  appears — that rule *replaced* five hand-written ones precisely because the list
  rotted and left seven contexts unfenced. `storefront` landed with no new rule.
  A **client fence** (mirroring the Redis/Kafka ones) is still not derivable, but
  it is for a *client library* confined to one adapter package — a driver holding
  a connection, credentials or a socket. Not every third-party jar: neither
  Thymeleaf nor jmustache is fenced, because a template engine is used at the
  presentation/config boundary by design, and a fence there would restate the
  layer rules ArchUnit already enforces.

## 9a. What a documentation test publishes

Every `document(...)` call writes to the API guide, so a documentation test is a
contract more than a test. The eleven-context audit that produced these ran
2026-08-16/17; each rule below cost at least one real defect, most of them two.

**The strict field check is narrower than it looks.** `requestFields` fails on a
field **present** in the payload and undocumented — never on a documented field
**absent** from it. So a fixture sending two of a record's six produces a two-row
table and a green build, and a client copying it omits the rest. `tour-operators/update`
published 2 of 6 that way while the guide's prose described a field the table
omitted. **Read the request record, not the fixture.**

**A stubbed null publishes the type `Null`**, which tells a client the field can
never hold anything. Any field your fixture leaves null needs an explicit
`.type(JsonFieldType.STRING)` beside `.optional()`. It ran to 19 across the repo.
Scan with `grep -r '^|`+Null+`$' target/generated-snippets --include='*-fields.adoc'` —
**`*-fields`, not `response-fields`**: request tables carry it too.

**`.optional()` publishes nothing.** The default template renders
`Path | Type | Description` with no Optional column, so a create table and its
PATCH come out byte-identical and the PATCH's fields read as mandatory. Put the
partial rule in the **description text**, and check what omission actually does —
several PATCHes here require every field and answer 422.

**Never restate a constant from `src/main`.** A description or stubbed message that
hand-copies an allowlist, a cap or a catalogue keeps publishing the old one after
the source changes, and the suite stays green because the test stubs the code it
copied from. Four instances: the media allowlist and its 25 MB cap, the avatar cap,
the menu depth cap, and two metafield type catalogues.

Two resolutions, in order of preference:
1. **Reword so the sentence carries no value at all** and point at the generated
   table or the published error that does. Nothing left to keep in step.
2. **Derive it** — make the constant public with an accessor beside it
   (`ContentType.ALLOWED`, `UploadMediaUseCase.MAX_BYTES`, `MenuItem.MAX_DEPTH`,
   `MetafieldType.codes()`), and build both the refusal and the description from it.

A predicate beats a second list: `MetafieldType.allowedAsMetaobjectField()` replaced
an inlined `== METAOBJECT_REFERENCE` in two use cases *and* a hand-written eight-code
description. **Prove it by mutation** — change the constant, rebuild, and check the
published output moved. Search `.adoc` as well as `.java`: one probe reported six
files moved and missed the seventh because it only searched test sources.

**Scan for refusals by where they are used, not by how long they are.** The duplicate-
literal scan these passes run had a `len >= 20` threshold, added because a short
fragment matched everything. It silently drops the single most duplicated shape in the
repo: `"Page not found"` is 14 characters, and so are `"Slot not found"` and
`"Menu not found"`. Three passes reported their context clean while carrying 3–5 copies
each; `page`'s eleven were only found because they had been noted by hand a pass
earlier.

The filter that works is semantic — any literal handed to an exception constructor,
**no length bound**:

```
grep -rEo 'new [A-Za-z]*(Exception|Error)\("[^"]{4,}"' src/main --include='*.java'
```

Run both: the length threshold for prose duplicated in descriptions and javadoc, this
one for refusals. A threshold chosen to cut noise is a threshold that decides what you
are allowed to find.

**And count occurrences, not files.** The scan de-duplicated within a file, so a class
saying one sentence four times reported as one hit. That is not a rounding error — it
inverts the priority, because **within-file repetition is where load-bearing sameness
lives**. `RefreshAccessTokenUseCase` throws `"Invalid refresh token"` four times, for
the unknown token, the replayed one, the missing user and the rotation-race loser: four
causes this API makes indistinguishable on purpose, with nothing holding them together
but four identical literals. It is the `TENANT_NOT_FOUND` case exactly.

Its 21 characters cleared the length threshold, so the scan *did* surface it — and the
shape it printed is why that did not help. **Two different fives coincide here, and
conflating them is the whole trap.** Over both trees, file-deduplicated, it printed
`[5x]`: five *files*, three of them tests, which reads as thin duplication spread
across a context. The production truth is five *occurrences* in **two** files, four of
them in one method chain. Restrict the broken scan to `src/main` and it prints `[2x]` —
the number that shows how little a file count sees.

Label every count with its scope and its unit. A total that means files in one place
and occurrences in another will eventually agree by accident, and that is the reading
nobody checks.

So the three ways one scan has been wrong, all scope and never logic: too long a
minimum, the wrong tree, and one occurrence per file. Print the per-file count.

**And a fourth, which points the other way: a verification scan looser than the claim it
checks manufactures false corrections.** Checking "audit is the only test hand-rolling the
error envelope" with `fieldWithPath("status").description` returned two more files — an
invitation's lifecycle `status` and a slot's `status`, neither an error field. The claim
was true; the check that appeared to refute it was the sloppy one, and acting on it would
have rewritten a correct statement into a wrong one. Match on something only the target
has (here: `status` *and* `error` *and* `timestamp` together).

**The sentence that says a thing was verified is itself a claim, and it is the one
that goes unchecked.** Two rounds of the category-FK review landed here rather than on
the design, which is what makes it a pattern and not an anecdote. First a javadoc said
a constraint-naming convention was "what every drop in this repository uses" — there
were five drops, none of them a foreign key, so the corpus held no instance of the case
at all. Then the sentence written to correct that one said "each mutation alone leaves
all ten green", when one of the two mutations was caught on its own — and the very next
clause said so.

Both were supporting evidence for a judgement that was right. That is the tell: the
design gets argued and re-read, while the *"I checked"* clause beside it is written
last, from memory of a run rather than from its output, and reads as authority
afterwards. **A claim about a corpus states the count and what it is a count of; a claim
about a mutation names which mutation and what failed.** If a sentence generalises over
runs — "each", "every", "always" — either it was measured per case or it is a guess
wearing a measurement's clothes. This is `PATTERNS.md` §9a's own rule ("count before
believing any number, including these") applied one layer up, to the prose that reports
the count.

The exposure is specific to this codebase: much of the safety here rests on javadoc
explaining *why* a guard is shaped as it is, and a reader who trusts that prose will not
re-run the check behind it.

**A message whose sameness is load-bearing gets written once and guarded.**
`"Tour operator not found"` was 20 literals in `src/main` and 16 in tests, said by
four different causes on purpose. `TenantNotFoundMessageIsWrittenOnceTest` is the
shape: walk **both** trees, fail on the literal anywhere but the constant's
declaration, and assert the walk actually visited files — a guard that scans nothing
passes loudly.

**Be precise about what it buys.** It stops the copies coming back; it does **not**
stop one site *diverging* to a near-miss sentence, which passes because the literal it
looks for is gone. Where indistinguishability is a security property, check whether
the structure already provides it — `TourOperatorMembershipPolicy.ensureMember` throws
once behind one predicate, so its two causes cannot differ whatever any string says.
Write the guard for the copies and credit the structure for the property.

**And exempt exactly one assertion, or you make the wording unverifiable.** A guard
that scans both trees forbids *pinning* the sentence as well as copying it — so after
the existing assertions are switched to read the constant they all become
tautological (`hasMessage(CONSTANT)` holds for any value), and a one-word edit changes
every published body with a green suite. That is strictly weaker than the copies it
replaced, and it is how it was found: rewording the tenant 404 moved **8 snippets**
silently. The resolution is one named pinning test exempted beside the declaration
(`TenantNotFoundIsThisSentenceTest`), asserting the literal once, with a failure
message saying which published bodies move.

**Make "did not widen" a test, not an instruction.** The earlier wording here told you to
plant the literal and watch the guard fail — a thing a human remembers or does not. It
also only checked one direction: a written-once guard that exempts a *file* lets a second
copy in through the back door, and "written once" becomes quietly false while the build
stays green. `RefreshTokenMessageIsWrittenOnceTest` closes both ends by counting
occurrences **inside** the exempted file and requiring exactly one — 0 means the wording
is pinned nowhere, 2 means the exemption widened.

**Both guards have it now, because the older one had the hole this describes.** The
tenant-404 guard exempted its pin by path and never looked inside, so replacing that pin's
literal with `TENANT_NOT_FOUND` — an edit that reads like tidying a literal away — left
`isEqualTo(TENANT_NOT_FOUND)` asserted against `TENANT_NOT_FOUND`, green, with eight
published bodies unpinned. That is the #183 defect regenerating itself. Written as an
instruction, this rule did not stop it; written as a test, it does.

**A stubbed placeholder becomes the published body.** `doThrow(new
ResourceAlreadyExistsException("exists"))` in a documentation test reads as throwaway
scaffolding and is not: the guide then tells clients a name conflict answers
`{"message": "exists"}`, and a missing row `"not found"`. `audience` published three
such bodies — `"exists"`, `"admin"`, `"not found"` — none of which any request can
produce, so a client matching on `message` never matched.

It also silently defeats centralising the message: the sentinel probe for
`AudienceOwnershipQuery.NOT_FOUND` reached **zero** snippets while the placeholder was
there, and two the moment the stub derived from the constant. **A stub that invents its
own message hides both defects at once** — the wrong contract, and the constant that
never arrives.

Stub what production actually throws: the constant where there is one, else the real
sentence. **`PublishedErrorBodiesAreSentencesTest` now fails the build on it** — a
stubbed message in a `*DocumentationTest` must start with a capital.

That rule is a proxy and says so. The real requirement is "is what production throws",
which cannot be checked mechanically: four real messages are *interpolated* and appear
nowhere in `src/main` verbatim (`"This action requires " + minimum + " privileges"` and
three more), so a verbatim-in-source check flags them all and is unusable. **Every
message that reaches a client** is a sentence — not every message in the repo, three of
which are lower-case `IllegalState`/`IllegalArgument` raised at wiring time — and every
placeholder was a bare token, so the initial capital separates them exactly.

**A guard's reach must match its claim, and this one's did not.** The first version
matched `new \w*Exception\("..."` — and `\w` excludes `.`, so a fully-qualified
constructor was invisible. It reported the repository clean while
`new com.vointika.shared.exception.ResourceNotFoundException("not found")` still
published `slots/list-not-found`. **The census that said "four offenders" had been taken
with the same pattern, so it inherited the blind spot it was measuring with** — and the
PR that added the guard edited that very file thirty lines above the survivor, trusting
the guard for the sweep. That is what a guard is for, and exactly why its reach has to be
proven, not assumed: plant the form you doubt and watch it fail.

**Know what it does not catch.** A plausible-looking wrong sentence passes: replacing a
placeholder with the wrong real constant published "Audience not found" for a non-member,
and this guard would have waved it through. That one needs the rule below.

**Fixing one is where it goes wrong: check which throw the stub stands in for, not which
noun the endpoint is about.** `audiences/list-not-found` stubs `ensureMember`, so its 404
is the *tenant* 404; reaching for `AudienceOwnershipQuery.NOT_FOUND` there published
"Audience not found" for a non-member. That is unproducible **and** worse than the
placeholder it replaced, because it confirms the operator resolved and the caller reached
the audience collection — the enumeration property `TENANT_NOT_FOUND` exists to protect.
`"not found"` is visibly fake; an authoritative-looking wrong sentence is not. The
siblings are the check: `media/list-not-found` and `tour-operators/members/list-not-found`
both publish `"Tour operator not found"`.

**A published error example must be reachable and must differ from its happy path.**
`PublishedExamplesAreHonestTest` fails the build on the second and cannot see the
first. Vary the thing the error turns on — a missing id for a 404, a STAFF token for
a 403 (that error is about who asks, so the URL stays), the clashing value for a 409.
Then walk the request back through the use case in the order it runs its guards:
`{"role":"OWNER"}` was published against "you cannot change your own role", which no
caller can reach, and a 422 about nesting depth was published with an empty array
that would have succeeded.

**Publishing only the errors that already have assertions misses half of every
symmetric pair.** `metaobjects/publish` had one and `unpublish` did not, so the guide
described a symmetry as one-sided.

**Verifying a shape does not verify its copies.** Both late passes worked by
replication — one translation table written and copied to three siblings, one path
description applied to every endpoint sharing a variable name. Each was right where
it was written and wrong somewhere else: metaobject fields are keyed bare, not
`namespace.key`; an upsert-only clause landed on a DELETE. Read each copy against
its own endpoint.

**A `{locale}` path variable does not mean the locale is validated.** Only the
`Upsert*` use cases ask — seven of them, all through
`shared.service.OperatorLocaleCheck`; reads answer `{}` and deletes 204. Say the 422
on the upsert and nothing on the others — per verb, not per section.

**And publish its message from `OperatorLocaleCheck.refusal(...)`.** Centralising the
*rule* is not centralising the *contract*: after the seven use cases were collapsed
onto one service, changing the message moved nothing in the guide, because five
documentation tests across five contexts each spelled the sentence themselves. One of
them published `"unsupported"` — a body no request can produce, so a client matching on
`message` never matched. **The probe that proves it is a one-line change to the source
plus a rebuild**: if no `response-body.adoc` moves, the contract is still copied.

Expect it to move **four** files, not seven: the two `metafield` upserts and one other
ask the check but publish no locale-422 operation, so they have no body to move. A
probe that moves fewer files than there are call sites is not evidence of a gap —
count the published operations, not the callers.

**Run the probe with a sentinel and `grep`, never `diff -r` on the snippet tree.**
Every error snippet carries a `timestamp`, so it is rewritten on every build: a
baseline-vs-rebuild `diff -rq` reported **~150 files "moved"** for a change that
touched two sentences, and the four that mattered were invisible in the noise.
Replace the message with something unmistakable (`"MUTATED-LIBRARY"`), rebuild, then
`grep -rl MUTATED target/generated-snippets` — the answer is the exact list of
published operations that carry it, and an empty answer is a real result rather than
a diff you gave up reading.

That empty answer is worth having on purpose. Collapsing six SEO value objects into
two, and rewording `page`'s three refusals to drop their `"Page "` prefix, grepped to
**zero snippets**: no SEO refusal is a published example anywhere, so the whole
cross-context change was invisible to clients. Knowing that *before* touching three
contexts is the difference between a refactor and a contract change.

**Errors are documented, not just happy paths.** Use
`ApiErrorSnippets.errorFields()` (`src/test/java/com/vointika/shared/web/docs/`) —
the shape is `status`, `error`, `message`, `code`, `timestamp`, there is no `path`,
and `code` is `@JsonInclude(NON_NULL)` so it needs `.type(STRING)` as well as
`.optional()`. An error a filter raises for the whole API (the 401) is documented
once, centrally; an error an endpoint's own rule raises belongs to that endpoint.

**Do not call `.snippets().withDefaults(...)`.** It *replaces* the default snippet
set rather than adding to it, so the operation stops publishing a curl and an httpie
example. It reached 61 operations across 18 classes, and no guard catches it —
`ApiGuideDocumentsEveryEndpointTest` checks that an operation is referenced, not what
it renders.

**There is no `relaxed*` documentation in this repository, and there should stay
none.** Each one suppresses the strict check.

## 10. Migrations

Per-context folder `db/migration/<ctx>/`, independent V-sequence, own Postgres
schema (`FlywayPerDomainConfig`). **Never modify an applied migration** — add the
next `V`. Curated reference/seed data lives in the migration.

**A migration that adds a NOT NULL column without a default must sweep
`docker/dev-seed/dev-seed.sql` in the same change.** The seed runs under
`psql -v ON_ERROR_STOP=1`. So the first INSERT that no longer matches the schema
aborts it, and **every INSERT after it never runs**. The symptom is not a seed
error.

V13 gave policies a surrogate id. The seed still inserted them without one, which
killed the file before the operator's OWNER membership and all three experiences.
On a recreated database the admin then signs in and *has no operators at all*,
which reads as the application losing data.

That is the third time this has bitten: `is_best_seller`, the storefront `status`,
and now the policy id. So state it as a step rather than a caution. **After any
migration that drops, renames, or adds a required column to a seeded table, grep
the seed for that table before opening the PR.** Only
`VointikaApplicationTests.contextLoads` runs migrations at all, and it does not run
the seed. Nothing in the build will tell you.

**You can still check it in one command, without Docker.** Run the seed against
the dev database inside a transaction that `TRUNCATE`s the seeded schemas first
and `ROLLBACK`s at the end: that is a true from-empty run — it catches the
ordering and NOT NULL faults a populated database hides, because an existing
parent row makes a mis-ordered child insert succeed — and the live data is
untouched. Running it three times in the same transaction is the idempotency
check (`ON CONFLICT DO NOTHING` converging means identical row counts). This is
what the build cannot do for you; it costs a few seconds.

**A seeded row that names a storage object must ship the object.** Media rows
whose MinIO object is missing render as broken images on every screen that
references them, which is why the seed carried none for so long. The pairing:
`docker/dev-seed/media/` holds one file per row named for the exact key suffix
the upload use case produces (`{mediaId}-{sanitized-name}.png`), and the
`minio-init` compose service uploads the directory under
`tour-operators/{operatorId}/` before anything reads it. Add a media row, add
its file — nothing checks the pair at build time **for media**; `EveryFlagKeyHasAFileTest` now does for flags, in both directions. **Country flags are the second instance**: `reference.country.flag_key` is `flags/{iso2}.svg`, which resolves against the same public base, so `docker/dev-seed/flags/` is uploaded to `avatars/flags/` by the same service. Only ES, US and DO ship one — the other 246 countries carry a NULL key rather than a key with nothing behind it.

**The seed can write what the API cannot, and now a test says so.** It inserts
straight into Postgres, so nothing stops it storing a value the domain would have
refused — and the build never runs it. That has bitten three times: two audit
rows naming actions no use case emits, and seven metafield keys using underscores
against a handle-shaped `MetafieldKey`. The last one surfaced as a **422 on one
endpoint and a 200 on its neighbour**, because list projections skip the value
object and detail reads construct it. `DevSeedWritesOnlyValuesTheDomainAcceptsTest`
now builds the seed's domain-shaped values with the real value objects and checks
its audit actions against the emitting code. **Add a seeded value that a value
object validates, and add it there** — and keep the minimum-count assertions, or
a pattern that stops matching turns the test into a no-op.

**A seed insert either converges or does nothing, and what the row is decides
which.** `ON CONFLICT DO NOTHING` keeps the old row while still reporting success.
So editing a value in the file changes nothing on an existing database, silently.
It cost three debugging rounds in two days, each time a working feature looking
broken because the fixture behind it was stale.

So: **configuration and authored content use `DO UPDATE`**, keyed on the id or the
natural key. **Records of things that happened keep `DO NOTHING`** —
`audit.audit_log` is append-only, and rewriting a trail entry in place would make
it a lie. A row that is nothing but its key has no value to converge, so
`tour_operator_locales` needs `down -v`; no conflict clause expresses a deletion.

The trade is explicit: anything changed locally through the admin API is reset on
the next `up`. That is right for a fixture. A seed you cannot correct is worse than
one that reasserts itself.

**A seeded reference row gets a literal, deterministic id — never
`gen_random_uuid()`.** `reference.country` uses UUIDv3 of `country:{code}`, so dev,
staging and prod agree on what `ES` is; random ids would make a `country_id` in an
audit row unresolvable across environments.

**What the seed is for is coverage, not plausibility.** A table with zero rows
renders exactly like a broken query, so a thin fixture makes whole admin screens
unreviewable. The rule: every table the admin UI reads gets rows, and every
*state* a screen can show gets at least one — published and draft, sold out and
cancelled, past and future, translated and not. Uneven on
purpose: if every owner has every optional field set, nothing shows you what
"unset" looks like.

## 11. Recurring gotchas (check before you trip)

- **A sub-resource segment must not collide with a sibling's path variable.**
  `/{owner}/metafields/translations` reads naturally and is wrong: it collides with
  `/{owner}/metafields/{namespace}/{key}`. `PathPattern` prefers the literal, so the
  route resolves — and silently makes a namespace called `translations` unreachable.
  A route that works by tie-break is one nobody remembers is fragile. The four
  translation mounts are `metafield-translations` and `field-translations` for this
  reason, and the rationale lived in three controller javadocs verbatim before it
  came here.
- Boot 4 autoconfiguration is per-starter: depend on the **Boot starter**
  (`spring-boot-starter-kafka`), not the raw library (`STACK.md` §gotchas).
- The autoconfigured `KafkaTemplate` is typed `<?, ?>` — inject the **raw**
  `KafkaTemplate`, not `<String, Object>`.
- Multi-line email templates end `</body>\n\n</html>` — assert `endsWith("</html>")`,
  not `</body></html>`.
- **Case-fold with `Locale.ROOT`, never the JVM default.** `"IT".toLowerCase()`
  under a Turkish default locale is `"ıt"` (dotless), so locale codes, handles and
  handles silently stop matching depending on which machine served the request.
  `LocaleCode` has always done this; `LocaleResolver` and `TenantHandleResolver`
  both had to be fixed to (the first is deleted, the second carries the comment).

  **`CaseFoldingUsesLocaleRootTest` now fails the build on it**, because writing the rule
  down did not stop it happening in four places. The third and fourth were found together
  in #194 — and the fourth only because the third prompted a sweep: `NotificationType.fileBase()` — every constant contains `EMAIL`, so all six folded
  to dotless and the template loader would refuse to start — and
  `RequestSizeLimitFilter`, where a client sending `MULTIPART/FORM-DATA` — legal, since
  RFC 9110 makes media types case-insensitive — stops being recognised and its upload gets
  the 1 MB JSON cap. (`Multipart/form-data` is fine: only *uppercase* `I` folds.)

  **The two are different kinds of exposure**, which is what to carry to the next case: a
  `SCREAMING_CASE` constant folds *by construction*, so no caller can avoid it; a header
  folds only for the casings a caller happens to send in caps.

  Both fail *closed* and both are invisible on every machine anyone develops or runs CI
  on, which is the whole shape of this bug: it is not caught by being careful, it is
  caught by the host having a locale you did not choose. The guard scans `src/main` only
  and skips comment lines, so the two places that *explain* the rule are not offenders.
- **A test whose subject reads the clock in a *stubbed* zone must build its dates
  in that same zone.** `CreateSlotUseCase` judges "is this in the past" against
  `LocalDate.now(zone)` for the **operator's** zone. That is correct: a departure is
  a wall-clock event where the tour runs.

  `SlotUseCasesTest` stubbed the zone to UTC and then built its dates from a bare
  `LocalDate.now()`, the machine's default. The two agree only while both name the
  same day. So the test failed **at 00:34 CEST** — Madrid had rolled over, UTC had
  not, so "yesterday" was still today to the use case and nothing was rejected — and
  passed again at 02:00. The window is midnight to the UTC offset, nightly.
  **The Docker build cannot catch this class of bug**: `eclipse-temurin` sets no
  `TZ`, so the container is UTC and the two zones can never disagree there. It
  surfaces only for whoever runs the suite locally from a non-UTC machine, which
  makes it read as a random red build. The fix is one constant feeding **both**
  the stub and every date the class builds (`OPERATOR_ZONE` + a `today()`
  helper), so the two cannot drift apart again. Prefer that to `Clock` injection
  until something needs to freeze time.
- **A storefront page route is registered in more than one place, and only the
  route itself fails loudly.** The `@GetMapping` is the route. `StorefrontPublicRoutes`
  needs **two** entries, GET and HEAD, because a `PublicRoute` matches one method —
  miss either and it is a 401 in the JSON error shape. **That is four registrations
  across three registries today**, because the password gate is live and its
  interceptor needs every page pattern too. Miss that one and a locked store serves
  the page to anyone.

  So define the pattern **once**, in `application/policy` where both layers can see
  it: `StorefrontRoutes`, with `LOCALIZED_*` built from `LOCALE` rather than
  retyping the regex. Then pin the rest with `servesHeadAsWellAsGet` per route.
- **A `PublicRoute` pattern is a security pattern first and a route second.** The
  same string that maps a handler decides what `permitAll` covers, and an
  unconstrained path variable is far wider as the latter: `/{locale}` opens
  **every single-segment path in the application**, silently, and keeps opening
  each new one. Constrain the variable (`/{locale:[a-z]{2}(?:-[a-z0-9]{2,4})?}`)
  and define it **once** — the mapping, the `PublicRoute` entries and any
  interceptor patterns must not drift apart. The group has to be **non-capturing**:
  `PathPatternParser` throws `IllegalArgumentException: No capture groups allowed
  in the constraint regex`; nested `{n}` braces are fine. Constraining a variable
  against a database allowlist creates a two-lists-must-agree coupling — pin it
  (`LocalePathTemplateTest`), or the day a wider code is seeded it is a 404 on a
  page the operator published and nothing says why.
- **`@AuthenticationPrincipal UUID` binds to null on a public route, silently.**
  `JwtAuthenticationFilter` stores the parsed `UUID`, so 141 controller parameters
  take one directly rather than re-parsing a String. On a **public** route an
  unauthenticated request arrives with Spring's anonymous principal, and
  `@AuthenticationPrincipal` defaults to `errorOnInvalidType = false` — so the
  wrong-typed principal becomes `null` with no error. That reads as "no session" by
  luck. `InvitationAcceptController` takes `@AuthenticationPrincipal Object` to say
  so in the type; do not "tidy" it to `UUID`.
- **Two classes with one simple name are one bean name, and the context refuses.**
  Component scanning derives the bean name from the simple name regardless of
  package. So `storefront.…StorefrontPasswordController` beside
  `touroperator.…StorefrontPasswordController` is a
  `ConflictingBeanDefinitionException` at startup, not a warning.

  Every sliced `@WebMvcTest` still passed. Only
  `VointikaApplicationTests.contextLoads` catches it, because it loads all thirteen
  contexts at once. Rename rather than reaching for an explicit bean name: the
  collision is the signal that one of the two names describes the wrong thing. Here
  it was the public gate page, now `PasswordPageController`.
- **A `WebMvcConfigurer` is pulled into *every* `@WebMvcTest`, not just its own
  context's.** So the collaborators an interceptor needs must be `ObjectProvider`s
  resolved per request, or every controller test in the codebase fails to construct
  the config. `WebConfig` (touroperator) does this, and its path patterns are what
  keep the resolution from ever happening on a foreign route. `StorefrontWebConfig`
  is the second example — it registers the storefront lock interceptor, which is
  live again since #138.
- An in-tx `save(entity)` followed by a bulk `@Modifying` JPQL on a **different**
  table needs `@Modifying(clearAutomatically = true, flushAutomatically = true)`.
  Without `flushAutomatically`, Hibernate skips the auto-flush (no query-space
  overlap) and the clear silently **discards the pending save**. Both snapshot
  propagators carry load-bearing comments on this — don't strip them.
