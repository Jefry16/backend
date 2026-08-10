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
`infrastructure` may not reach each other, so a helper the *controller* uses and
the *config* wires cannot live in either — put it in `application` as a plain
POJO and `@Bean` it from the context's config. `storefront`'s
`TenantHandleResolver` was written in `infrastructure/web` first and ArchUnit
rejected it in three places; it takes the host as a `String` and knows nothing
about servlets, so `application/policy` is where it belongs anyway.

**`application/policy` is now the settled home for that kind of rule** —
`TenantHandleResolver` (host → tenant) and `LocaleResolver` (path locale +
operator config → the locale to render) are both there. A policy that holds
configuration is an instance `@Bean`ed from the config; a pure function of its
arguments is `static` with no bean at all (`LocaleResolver`), and the choice is
just whether there is state to inject. **A constant two layers need also goes
here** — the storefront's unlock-cookie name lives on `UnlockTokenPort` for
exactly that reason: the interceptor that reads it and the controller that writes
it cannot see each other.

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
  response, and calling it one would mislead. Canonical: `storefront`'s `HomeView`.

## 2a. The render envelope (a server-rendered page's context object)

A page a template renders takes **named objects, never a flat bag of scalars**,
and the same set on every page. `storefront` is the canonical one:

```
shop          id, name, address, phone, email, url, description, passwordMessage,
              brand { slogan, shortDescription,
                      colors { primary [ {background, foreground} ], secondary [ … ] },
                      logo, squareLogo, favicon, coverImage,   -- Image or null
                      socialLinks [ { platform, url } ] },
              policies [ { type, title, url } ],
              cancellationPolicy, privacyPolicy, termsOfService, legalNotice,
              currency { code, symbol }, timezone { name, city }
page          title, description, ogImageUrl, path
routes        root, experiences
localization  locale, languages [ { code, current, url } ]
```

**A named accessor beside a list is Shopify's shape and is worth copying.**
`shop.policies` iterates; `shop.cancellationPolicy` is the one a booking form
wants without comparing type strings, and is **null** when the operator has not
written it, so a template guards on the object. The four names are not derived
from the type — `TERMS` is `termsOfService`, because that is what a theme author
coming from Shopify types.

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

**There is no `shop.logoUrl`.** The logo is `shop.brand.logo`, where Shopify
keeps it — their shop object has no logo of its own. Removing it was a breaking
change to a published contract, made deliberately while no operator theme
existed to break (#100).

It exists **twice, in key form and URL form** — `ShopData`/`PageData`/
`BrandData`/`ImageData`/`LocalizationData` under one `StorefrontPageData` in
`application/dto/output`, and `Shop`/`Page`/`Brand`/`Image`/`Routes`/
`Localization` in `presentation/view`. That is PATTERNS §5 applied to a page:
application deals in storage keys and locale codes, presentation resolves both
(`routes` has no application half at all — a route is a URL, and `aspectRatio`
is derived on the same side for the same reason). Every one is a `public record`
with a `public` enclosing type, and so is every nested one, because the compiler
runs with access coercion off.

**A collection the owning context orders is ordered by the query, and split by
the query too.** The palette is `colors.primary[0].background`, so its order is a
promise a theme indexes into — it lives in the derived query's name
(`findByTourOperatorIdOrderByPositionAsc`) and nowhere else, pinned by parsing
that name with Spring Data's `PartTree` (§9's shape, from the experiences
listing). And the *role* split happens in the owning context's adapter, not the
caller's: a role is a `touroperator` enum, so a flat list tagged with a role
string would force `storefront` to compare against literals — a second copy of an
enum it is fenced from seeing.

**One rule decides what goes in: expose what the row has, invent nothing.** A
field with no column behind it is invention and stays out; a field with a column
goes in whether or not this slice renders it.

That used to be two rules — the second demanding "a renderer in this slice or a
named caller in the next", which is what kept `shop.timezone` out. It was right
for a page and wrong for a contract, and #96 dropped it: `shop` is API the day an
operator authors a theme, so a field added later is a breaking change while a
field added now costs one record component. `shop.timezone` is in. So are
`brand.slogan` and the palette, which nothing renders yet — the shape is the
contract and the data follows. A field is omitted only when no column backs it,
or when it belongs somewhere else (theme settings, `localization`).

**A contract filled in data-first needs a way to see it.** Most of `shop` is
invisible in the page, so `?format=json` (`ThemeContextDump`, off unless
`app.storefront.context-endpoint` says otherwise) renders the object a template
would receive instead of the page. It is the diagnostic that makes this rule
verifiable against a running system rather than only against a test.

**A page-specific record wraps the envelope rather than flattening it**
(`ExperienceListPageOutput(StorefrontPageData envelope, List<ExperienceCard>
cards)`), and a page with nothing of its own returns the envelope directly —
the home page does.

**The four top-level components are repeated across every page view, and that is
accepted.** Records cannot extend, and nesting them would put
`{{envelope.shop.name}}` in every template. Revisit when sections make the
render context globals-plus-a-section — likely a `Map` — which is the first real
second consumer; do not build the map before it.

**Where a URL that varies per page is built:** `application` says *where* a thing
lives (a nullable `pathLocale`, null for the locale that serves bare),
`presentation` says *what its URL is*. The language switcher needs "this page in
that language", which differs per page, so `Localization.from` takes a
`Function<Routes, String>` and each view passes `Routes::root` or
`Routes::experiences` — one prefix rule, in `Routes`, and no page hard-codes a
path.

**Renaming a component here is a breaking change** once operators author themes.
Decide the shape while there are four records to change, not forty templates.

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

**Nested-only variant:** a reference type used *only* inside another response
(e.g. `Country` nested in a timezone) keeps entity + JpaEntity + Mapper +
Response and **drops** the repository / use case / controller. Don't add a
standalone endpoint until something needs it.

Canonical: `reference` — `Timezone`/`Currency` full, `Country` nested-only.

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

Any list over **tenant or growable data** (members, bookings, orders, audit) MUST
use the shared list framework — **never an unbounded array** (the roster shipped
that way once and it was the recorded mistake this fixes). The recipe:

1. **Schema** — a `public static final ListSchema SCHEMA` on the use case:
   `.tenantScoped()` (scopes to the entity's `tourOperatorId`), `.set/text/number/
   instant(...)` for each filterable field, `.sortable(...)` + `.defaultSort(...)`.
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
Canonical: `ListMembersUseCase` + `GET /api/tour-operators/{id}/members`.

## 4d. Two namespaces read as one must be validated as one

A storefront handle resolves against **localized handles first, canonical handles
second**. That makes them one namespace on the read side, so uniqueness has to be
checked across both on every write — otherwise one silently shadows the other and
the shadowed page becomes unreachable in that locale, with no error at any point.

> **The read half is gone entirely (2026-08-02).** The whole storefront serving
> side was deleted — the `rendering` context and all four `Storefront*Query` seams
> — so nothing in this codebase resolves a handle at all today. Whatever renders
> the public site next has to build that read path, and this section describes the
> rule it must honour.
> **The write guards below were kept anyway.** They cost nothing, and dropping them
> would let a shadowing handle be stored while nothing can observe it, surfacing as
> an unreachable page the day a read path returns — a defect committed now and
> discovered much later. Restore this section's present tense with that read path.

`page` shipped with each namespace checked only against itself, which is the natural
mistake: the create/rename path asks `pages`, the translation path asks
`page_translations`, and each looks complete on its own. The three write paths now
cross-check:

- **create / rename a canonical handle** → also reject it if any *other* page uses it
  as a localized handle in **any** locale;
- **upsert an explicit localized handle** → also reject another page's canonical handle;
- **derive a localized handle** → probe *both* namespaces, so the auto-suffix never
  lands on one either.

Matching the page's **own** canonical handle is fine — it resolves to the same page.
The general rule: when a read path consults two sources in precedence order, list the
write paths that feed each and make every one of them check both.

`experience` had the same defect and now has the same guards, with one difference worth
knowing before you read the two side by side and think one is wrong. **Whether a
cross-namespace collision is a 409 or a suffix depends on who chose the value, not on
which namespace it came from.** A page handle is operator-chosen and permanent, so a
clash is a 409 the operator can act on. An experience's canonical handle is *derived from
its name*, so its create path widens the probe instead — the auto-suffix simply steps
over localized handles too, and the operator sees a `-2` rather than a 409 for a value
they never typed and have no field to correct. The explicit localized handle is
operator-chosen in both, and 409s in both.

One consequence: the any-locale probe needs an exclusion parameter only where a *rename*
path calls it (page). Where the canonical value is immutable (experience), create is the
only caller and never excludes — so the parameter, and page's nil-UUID sentinel standing
in for "exclude nothing", are both absent by LAW §2.4.

**These guards are pre-checks, not constraints, and that is the one thing this recipe
cannot fix.** Uniqueness *within* a namespace is backed by a unique index, so a lost race
surfaces as a duplicate-key failure and the loser is rejected. There is no index spanning
the two tables and there cannot be one without a trigger — so two concurrent writes, one
per namespace, can still land on the same value and produce exactly the shadowing the
guards exist to prevent. The window is small and both `page` and `experience` carry it.
Treat the cross-namespace check as closing the reachable-by-one-request hole, not as
making the invariant true.

## 4e. The translation-overlay table (six of them, in two shapes)

A translatable aggregate gets a sibling table keyed on
`(<owner keys…>, locale)`, and the read overlays it
**nullable-wins-canonical**: a null column falls back to the owner's own value,
never to an empty string. A row overlays; it does not replace. Translating a
title and not a body is a Spanish title over an English body, which is the
realistic partial-translation case and the one fallback bugs hide in.

**Six tables do this**, not the four this section used to claim:

| | owner key | content columns | overlaid by | clearing |
|---|---|---|---|---|
| `experience_translations` | single id | 9, nullable | `StorefrontExperienceQueryImpl` | blank → null |
| `tour_operator_translations` | single id (the operator) | 5, nullable | `StorefrontShopQueryImpl` | blank → null |
| `tour_operator_policy_translations` | **composite** `(operator, type)` | 2, nullable | `StorefrontShopQueryImpl` | blank → null |
| `page_translations` | single id | 5, nullable | **nothing yet** | blank → null |
| `audience_translations` | single id | 1, nullable | **nothing yet** | blank → **delete** |
| `menu_item_translations` | single id | 1, **NOT NULL** | **nothing yet** | **blank → 422** |

**Half of them are never resolved to a locale**, because nothing renders them
translated yet — there is no CMS page route, audiences are not on the storefront
and menus are not rendered. They have full admin CRUD and no reader. That is not
a gap to fix; it is what "the write half shipped first" looks like. The admin
deliberately returns *every* locale rather than a resolved one — `GetMenuUseCase`
hands back a locale→title map — which is what an editor needs.

Same one level down: only **3 of experience's 9** translatable columns are read
(`handle`, `name`, `description`), because the listing card is the only consumer.
The rest are stored and never rendered in any locale.

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
written.** Its items are not editable individually — the whole tree is POSTed and
rebuilt with fresh ids — so translations ride inline in that payload, have no
endpoints of their own, and are cleared by being left out. It is also the only
one without a `tour_operator_id`; items are always reached through their menu, so
adding one would be a migration for a join nobody needs.

**Still not generalising, but the old reason no longer holds.** This section used
to argue they differ on three axes. They do not: every table that actually
overlays does it in a storefront query adapter, with the same two-line helper
(`translated != null ? translated : canonical`), and clearing is uniform. Only
the key shape still differs — policies are the lone composite, and they key on
`type` rather than the surrogate `id` V13 gave them. What is left to share is two
lines at three call sites, and sharing it across contexts means pushing it into
`shared` — real coupling to remove six lines (LAW §2.4). Keep the **rule**
identical, not the code.

Revisit when something renders pages, audiences or menus translated: that triples
the overlay sites at a stroke, and *then* the duplication is worth a second look.

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
> transient failures. A **must-not-drop** event (payment, refund, booking state)
> must **not** log-and-swallow: it needs a different shape — at-least-once +
> idempotent consumer + retry/DLQ — whose decided direction is in the **MAP
> backlog** ("Critical-event delivery"). Build that shape when the first such event
> lands, not before (§2.3).

## 8. Config-driven capability (grow by config, not code)

A capability whose *set* grows over time (UI languages, email locales) is a
config allowlist (`@ConfigurationProperties`), validated in the use case, and
exposed via a read endpoint when the frontend needs the list. Growing it = add a
config key (+ any assets like a template file); no migration, and no code
*for the capability itself*.
Canonical: `app.identity.ui-languages` + `GET /api/ui-languages`.

**Count the consumers of the allowlist before calling it config-only.** Adding a
UI language is a yml edit for the picker and for validation — but transactional
email keeps its own list (`ClasspathTemplateCatalog.LOCALES`) plus a template pair
per (type, locale), and a language missing from it does not fail: the send falls
back to English, so the user silently gets the wrong language. A second list that
must agree with the allowlist needs a test that fails the build when they diverge
(`TemplateLocalesTrackUiLanguagesTest`), not a comment saying it should track.

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
`shared.port.AuditTrailPort.append(NewAuditEntry)` — **inside the same
`TransactionRunner` block as the mutation**, so the entry commits and rolls
back atomically with the action ("no unaudited mutation"; a failed append fails
the action). Exception: a mutation whose target is object storage (S3) appends
in its own transaction AFTER the successful write — storage can't roll back, so
that is the honest best-effort. Actor name is frozen at write (filter-only,
never sortable — it's nullable and keyset cursors need non-null sort keys).
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
fails the build exactly like a third-party jar would. `storefront`'s unlock cookie was
written as an `application/policy` class on the assumption that "pure JDK" was enough —
`UnlockTokenPort` + `HmacUnlockToken` in `infrastructure/security` is what it became.
Reading the rule says this; only running it proves it, which is the point.

**Logging follows the same rule.** If a *side effect* fails and the caller does not
care — deleting an object whose row is already gone, enqueuing a welcome email — the
adapter swallows and logs it, and the port documents that it never throws. Only when
the use case itself has something to report (a security signal, a branch taken because
config was missing) does it reach for `DiagnosticLogPort`, which takes the calling
class so log names still point at the reporter.

## 4c. One DTO or two at the application boundary

A use case takes an `Input` from `application/dto/input` and the controller owns a
`Request` in `presentation/request`. Keep both **only when they differ**. In identity
nine pairs do — the `Input` carries a `userId` from the authenticated principal, or a
`language` the body never had — and four were byte-identical copies, since deleted.

An identical copy is not a seam. Add a field to the request that the use case needs
and both change in lockstep, so it insulates nothing while costing a file and a
mapping step.

**Check the nested records separately from the wrapper.** `ReplaceMenuItemsRequest`
and `ReplaceMenuItemsInput` genuinely differ — the input adds the caller and the two
path ids — but the tree node inside them was identical, so the controller carried a
recursive copy that ran on every save. A pair can be a real seam at the top and a
pure copy one level down; the nested type is where the cost is, because collapsing it
deletes a mapper and not just a file.

When you collapse one, **the application record is the survivor** and the controller
binds to it:

```java
public ResponseEntity<LoginUserResponse> login(@RequestBody LoginUserInput input) {
    var output = loginUserUseCase.execute(input);
```

Never the other way. A use case referencing a `presentation` type inverts the layer
graph and ArchUnit fails the build.

The condition, and the build enforces it: the surviving record must carry **no
annotations**. The application layer's allowlist is `com.vointika..` + `java..`, so a
`@JsonProperty` or a Jakarta validation annotation on it is a compile-time-legal but
build-breaking change — and the correct answer at that point is to reintroduce a
presentation DTO, because the shapes have genuinely diverged.

Responses are the mirror image: `LoginUserOutput` carries `accessToken` *and*
`refreshToken`, `LoginUserResponse` carries only the access token because the refresh
token leaves in an httpOnly cookie. That pair stays.

## 9. Testing shapes

- **Unit** — JUnit5 + Mockito, no Spring: every value object, entity behavior,
  and use case (inline `TransactionRunner` lambda for the tx). Carry the
  security-critical cases (anti-enumeration, timing parity, token rotation).
- **Controller documentation test** — `@WebMvcTest(FooController.class)` +
  `@Import(SecurityConfig.class)` (add `IdentityPublicRoutes` when testing a
  public route), collaborators `@MockitoBean`, assertions + RestDocs
  `document(...)`. Authenticated endpoints send `Authorization: Bearer …` and
  stub `AccessTokenValidatorPort.isValid/extractUserId`.
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

  **Write it as a biconditional, not a one-way assertion.** Two of these began
  as "everything is read-only, because nothing writes it" and had to **invert**
  when the write path landed — and the biconditional form inverts by itself, so
  the slice that adds the writer flips the test by adding the domain accessor
  rather than by editing the test. A one-way `isFalse()` has to be rewritten,
  and a test you rewrite alongside the change it guards has guarded nothing.

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

## 10. Migrations

Per-context folder `db/migration/<ctx>/`, independent V-sequence, own Postgres
schema (`FlywayPerDomainConfig`). **Never modify an applied migration** — add the
next `V`. Curated reference/seed data lives in the migration.

**A migration that adds a NOT NULL column without a default must sweep
`docker/dev-seed/dev-seed.sql` in the same change.** The seed runs under
`psql -v ON_ERROR_STOP=1`, so the first INSERT that no longer matches the schema
aborts it and **every INSERT after it never runs** — and the symptom is not a
seed error. V13 gave policies a surrogate id; the seed still inserted them
without one, which killed the file before the operator's OWNER membership and
all three experiences. On a recreated database the admin then signs in and *has
no operators at all*, which reads as the application losing data.

That is the third time this has bitten (`is_best_seller`, the storefront
`status`, now the policy id), so the rule is worth stating as a step rather than
a caution: **after any migration that drops, renames, or adds a required column
to a seeded table, grep the seed for that table before opening the PR.** Only
`VointikaApplicationTests.contextLoads` runs migrations at all, and it does not
run the seed — nothing in the build will tell you.

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
its file — nothing checks the pair at build time.

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

**What the seed is for is coverage, not plausibility.** A table with zero rows
renders exactly like a broken query, so a thin fixture makes whole admin screens
unreviewable. The rule: every table the admin UI reads gets rows, and every
*state* a screen can show gets at least one — published and draft, sold out and
cancelled, past and future, translated and not. Uneven on
purpose: if every owner has every optional field set, nothing shows you what
"unset" looks like.

## 11. Recurring gotchas (check before you trip)

- Boot 4 autoconfiguration is per-starter: depend on the **Boot starter**
  (`spring-boot-starter-kafka`), not the raw library (`STACK.md` §gotchas).
- The autoconfigured `KafkaTemplate` is typed `<?, ?>` — inject the **raw**
  `KafkaTemplate`, not `<String, Object>`.
- Multi-line email templates end `</body>\n\n</html>` — assert `endsWith("</html>")`,
  not `</body></html>`.
- **Case-fold with `Locale.ROOT`, never the JVM default.** `"IT".toLowerCase()`
  under a Turkish default locale is `"ıt"` (dotless), so locale codes, handles and
  handles silently stop matching depending on which machine served the request.
  `LocaleCode` has always done this; `LocaleResolver` had to be fixed to.
- **A test whose subject reads the clock in a *stubbed* zone must build its dates
  in that same zone.** `CreateSlotUseCase` judges "is this in the past" against
  `LocalDate.now(zone)` for the **operator's** zone — correct, a departure is a
  wall-clock event where the tour runs. `SlotUseCasesTest` stubbed the zone to
  UTC and then built its dates from a bare `LocalDate.now()`, i.e. the machine's
  default. The two agree only while both name the same day, so the test failed
  **at 00:34 CEST** — Madrid had rolled over, UTC had not, so "yesterday" was
  still today to the use case and nothing was rejected — and passed again at
  02:00. The window is midnight to the UTC offset, nightly.
  **The Docker build cannot catch this class of bug**: `eclipse-temurin` sets no
  `TZ`, so the container is UTC and the two zones can never disagree there. It
  surfaces only for whoever runs the suite locally from a non-UTC machine, which
  makes it read as a random red build. The fix is one constant feeding **both**
  the stub and every date the class builds (`OPERATOR_ZONE` + a `today()`
  helper), so the two cannot drift apart again. Prefer that to `Clock` injection
  until something needs to freeze time.
- **A storefront page route has to be registered in four places, and only one of
  them fails loudly.** The `@GetMapping` is the route; `StorefrontPublicRoutes`
  needs **two** entries, GET and HEAD, because a `PublicRoute` matches one method
  (miss either and it is a 401 in the JSON error shape); and
  `StorefrontWebConfig` needs the pattern too, or a locked store serves the page.
  Only the first is visible without a test. So define the pattern **once** — in
  `application/policy`, where both layers can see it (`StorefrontRoutes`, built
  from `LocaleResolver.PATH_TEMPLATE` rather than retyping the regex) — and pin
  the other three: `servesHeadAsWellAsGet` and a locked-store test per route.
  Canonical: `storefront`'s `/experiences` pair.
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
- **Two classes with one simple name are one bean name, and the context refuses.**
  Component scanning derives the bean name from the simple name regardless of
  package, so `storefront.…StorefrontPasswordController` beside
  `touroperator.…StorefrontPasswordController` is a `ConflictingBeanDefinitionException`
  at startup — not a warning. Every sliced `@WebMvcTest` still passed; only
  `VointikaApplicationTests.contextLoads`, which loads all thirteen contexts at once,
  catches it. Rename rather than reaching for an explicit bean name: the collision is
  the signal that one of the two names is describing the wrong thing (here the public
  gate page, now `PasswordPageController`).
- **A `WebMvcConfigurer` is pulled into *every* `@WebMvcTest`, not just its own
  context's.** So the collaborators an interceptor needs must be `ObjectProvider`s
  resolved per request, or every controller test in the codebase fails to construct
  the config. Both `WebConfig` (touroperator) and `StorefrontWebConfig` do this, and
  their path patterns are what keep the resolution from ever happening on a foreign
  route.
- An in-tx `save(entity)` followed by a bulk `@Modifying` JPQL on a **different**
  table needs `@Modifying(clearAutomatically = true, flushAutomatically = true)`.
  Without `flushAutomatically`, Hibernate skips the auto-flush (no query-space
  overlap) and the clear silently **discards the pending save**. Both snapshot
  propagators carry load-bearing comments on this — don't strip them.
