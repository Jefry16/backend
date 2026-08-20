# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read these first

**Nothing loads these for you.** A handful of javadoc comments name `PATTERNS.md` and `STACK.md`, so the code knows they exist — but no import pulls one in and no build step reads one. Load them yourself:

| Document | Location | What it is |
|---|---|---|
| **LAW** | `../CONSTITUTION.md` (the parent directory — its own git repo) | The rules. Short, read whole, every session. |
| **PATTERNS** | `PATTERNS.md` (in repo) | The recipes. Before building anything, find the matching one — don't reverse-engineer existing code. |
| **STACK** | `STACK.md` (in repo) | Every pinned dependency → its version → its official docs URL. |
| **API-DOCS-SYNC** | `API-DOCS-SYNC.md` (in repo) | The playbook for checking one context's API against the REST Docs guide, for a context built from here on. The eleven-context series it was written for closed 2026-08-17 (`storefront` excluded by decision); what it settled is `PATTERNS.md` §9a. |

LAW §4 is absolute and worth restating: **never assume — verify or ask.** Version-specific behavior goes to the pinned version's docs, never to recall (Boot 4 differs from Boot 3 in ways that cost real debugging time — see `STACK.md` gotchas). And a claim that something is unused or removable is produced by deleting it and running the suite, not by reading it.

## Commands

```bash
./mvnw test                                  # full suite
./mvnw test -Dtest=ClassName                 # one class
./mvnw test -Dtest=ClassName#methodName      # one method
./mvnw test -Dtest='Foo*Test,BarTest'        # patterns / several
./mvnw -o test                               # offline, once deps are cached — noticeably faster
./mvnw package                               # jar + REST Docs snippets + the asciidoctor API guide
```

Java 25, Spring Boot 4, Maven wrapper. Jacoco is bound to the build.

**The whole stack runs on Docker Compose** — Postgres 17, Redis, Kafka (KRaft), MinIO, a local SESv2 mock, the app, and a one-shot dev seed:

```bash
docker compose up --build          # app on :8080; Flyway migrates every domain schema on boot
docker compose run --rm app <cmd>  # one-off against the live DB (validates Flyway + ddl-validate)
```

- Dev credentials: `admin@vointika.test` / `password`; the seeded operator is `acme`.
- Sent email is readable at `GET localhost:8005/store` (the SES mock).
- `docker compose build app` alone **skips the live-DB path** — it won't catch a Flyway or `ddl-auto: validate` failure. Use `docker compose run` for that.
- Never pipe `docker compose build` into `tail`/`grep`: it masks the exit code and you'll ship a stale image.

### Running the suite locally

`target/` is sometimes left root-owned by the Docker build, which breaks `./mvnw` on the host. Either `sudo rm -rf target/`, or copy the tree elsewhere and build there:

```bash
rsync -a --delete --exclude target/ --exclude .git/ ./ /tmp/scratch/be/
rm -rf /tmp/scratch/be/target/classes /tmp/scratch/be/target/test-classes
```

That second line matters: `rsync -a` preserves mtimes, so a stale compiled class can look newer than the source that replaced it and Maven silently skips recompiling — producing a **false test failure against code you already fixed**. Clear the classes whenever you re-sync.

## Where this stands

Clean rebuild. **Thirteen contexts** are built — identity · notification · reference · touroperator · media · experience · audience · pickup · audit · page · metafield · contact · storefront — on the `shared` kernel. Trunk is `main`; every PR merges with the full suite green, and the `docker compose up` runtime gate is the user's to run.

**Built:** the admin API in full, the operator's own surface (brand, store policies, structured address, SEO, translations, locales, menus, custom data), and the storefront's data contract. **Not built:** the transaction half — cart → checkout → bookings — and everything that makes a storefront look like one, since `storefront` still answers JSON rather than HTML. Themes, sections, the experience detail page and the theme model are all open.

The storefront was built, deleted whole on 2026-08-02, rebuilt in-process, and cut back to a placeholder on 2026-08-11. **Treat those deletions as history, not as the current shape** — the live contract and render path are `PATTERNS.md` §2a and §2b. The deleted separate-renderer repo is a git bundle outside the tree (`~/storefront-archive.bundle`, 20 commits); it had no remote.

**The admin frontend** (`../frontend`, branch `staging`, React 19 · TanStack) keeps pace slice for slice. Its structure and rules live in its own repo. Its role-gating is cosmetic — `TourOperatorMembershipCheck` is the only real gate.

## Architecture

A modular monolith: `com.vointika.<context>`, one package per bounded context, each **fully hexagonal** with the layer DAG `domain ← application ← {infrastructure, presentation}`. `domain` is pure — no Spring, JPA, or Jackson. A context that owns no entities has no `domain` (`notification`, `storefront` — PATTERNS §1); the DAG is unchanged, and in those contexts it bites harder, because `presentation` and `infrastructure` still may not reach each other.

**ArchUnit enforces the boundaries** (`src/test/java/com/vointika/architecture/ArchitectureTest.java`), so a violation is a failing test, not a review comment. Context isolation is **derived from the package structure**, so a new context is fenced the day its package appears. There is no rule to remember to add. It used to be one hand-written rule per context. Seven contexts then landed without one, and the rules that did exist only named the original four.

### What each context owns

The context set is **designed as we go**, not ported wholesale — each earns its boundary (LAW §2.2). `identity` was a deliberate exception: full hexagonal, full parity, reproducing the archived design faithfully.

| Slice / context | Owns | Status |
|---|---|---|
| `identity` | Accounts, credentials, JWT + rotating refresh sessions, email verification, password reset, profile, avatar, UI language, invited-user provisioning. `/api/auth/**` + `GET /api/ui-languages` (authenticated). | Built |
| `notification` | Transactional email — consumes identity events off Kafka, renders Thymeleaf templates (classpath, **en + es**), sends via SES **in the recipient's locale** (carried on the event). No HTTP surface. | Built |
| `reference` | Read-only reference data. **Countries** (249 — the full ISO 3166-1 list since V6; `flag_key` is **nullable** and only ES/US/DO carry one, so `flagUrl` is null for the other 246) + **timezones** (4, each with a nested country) + **currencies** (3: DOP/EUR/USD). Endpoints: **`GET /api/countries`**, **`GET /api/timezones`**, **`GET /api/currencies`** (authenticated; `/api/languages` is the row below). **`/countries` was removed and then restored** — the structured postal address needs an addressable country list, and three rows cannot address a business. Shared-kernel-like: importable by any context; imports nothing but `shared` (the flag-URL resolver). Both directions are ArchUnit-enforced — the generic `slices()` rule ignores `reference` as a dependency *target* but not as a source. | Built + merged |
| `touroperator` | The tenant aggregate, its team, and everything an operator configures about itself. **Create** (`POST /api/tour-operators`, creator→OWNER, operator + owner in one tx; **single owner per operator** and **one default operator per user** are DB-enforced via partial unique indexes; welcome email). **Read/edit** `GET`+`PATCH /api/tour-operators/{id}` over name, address, phone, email, timezone and currency — `handle` is deliberately absent, it is the storefront subdomain. **The address is structured** (`address1`, `address2`, `city`, `province`, `zip`, `country_id` → `reference.country`) and **PATCH replaces it whole**: merging `{"city": "Barcelona"}` into a Madrid address would produce a plausible-looking wrong address rather than a partial one. **Team:** invitations (invite/list/get/resend/revoke, ADMIN+-write member-read; public preview/accept with verified-from-birth provisioning) + members (roster, role change, ownership transfer, remove/leave). **The membership view carries resolved reference values, not ids** — the IANA timezone name and the ISO 4217 currency *code*, not the symbol, because `Intl.NumberFormat` derives symbol and per-locale decimals from the code. Sub-resources, each one-per-operator and so addressed without an id: **`/brand`** (slogan, short description, four media ids, and two ordered child tables — the palette is `colors.primary[0].background`, so **position comes from the payload order and is an address a theme indexes**; social platforms are a closed list retargeted away from Shopify's nine to the ones an operator actually runs). **`/seo`**, **`/locales`**, **`/translations/{locale}`** (composite PK, every column nullable so it overlays; `name`/`handle`/`address` deliberately untranslated — a brand name is not content). **`/policies/{type}`** over a closed four-value `PolicyType` — **here and not in `page`** because the defining trait is one per operator from a fixed set. **`/storefront-password`** — `password_enabled` + a **plaintext** password by design (a shared gate the operator reads back and hands out, Shopify's model, never a credential), and **the password value never reaches the audit trail**. **Menus** — `menus` (immutable handle, renameable title) + a self-referencing `menu_items` tree, position-ordered, 3-level cap, link types HOME/EXPERIENCE_LIST/EXPERIENCE/PAGE/EXTERNAL_URL validated against the ownership ports; `PUT .../menus/{id}/items` **replaces the whole tree wholesale** (fresh ids, one audit event). Every operator gets `main-menu` + `Footer` at creation. Both enum pairs are pinned to their CHECK constraints **both ways** — enum-not-in-CHECK fails the write with an untranslated 23514, CHECK-not-in-enum fails a read on a public page. Still no status or fee fields. | Built + merged |
| `audience` | Pax pricing tiers (Adult, Child, …) — own context, operator-scoped, reused across slots. CRUD minus delete under `/api/tour-operators/{id}/audiences` (member-read/admin-write; name unique per operator **case-insensitive**, 409). Update is PARTIAL and propagates name/paxPerUnit onto slot snapshots via `SlotAudienceSnapshotPropagator` (#47). Per-locale **name translations** (#52, 4 endpoints mirroring experience translations). **DELETE removed (#53)** pending a product decision on deletion-vs-snapshot semantics. Ships `AudienceOwnershipQuery` for experience. | Built + merged (#45/#47/#52/#53) |
| `experience` (slots) | + **Slots + per-audience pricing** (#46/#48): a slot = a bookable departure, operator-LOCAL wall-clock `startAt`/`endAt` (`LocalDateTime`, duration derived, ≤24h, cross-midnight self-describing), snapshots experience name/description (kept in sync on experience edit, #48); timing immutable — cancel + recreate. Status AVAILABLE/SOLD_OUT/CANCELLED, and **none of the three is an operator toggle**: SOLD_OUT is derived from bookings at checkout (nothing writes it yet), CANCELLED has its own endpoint, and the slot PATCH edits capacity only. `audience_slot` rows: price+capacity **frozen at create**, name/paxPerUnit synced, bare `audience_id` (no FK — outlives deletion), `booked_count` (0 until bookings exist; DB CHECK ≤ capacity). 6 endpoints: single + recurring create, list (soonest-first), get, cancel, PATCH status/capacity. Also **raw media refs exposed on reads** (#44: `thumbnailMediaId` + `mediaIds`). | Built + merged |
| `pickup` | Pickup locations **catalog only** (name unique per operator case-insensitive + local meeting time). 5 CRUD endpoints under `.../pickup-locations` (partial PATCH; `context:"pickup-locations"`). **A standalone catalog is the finished state, not a stepping stone** — slots know nothing about pickups and nothing is waiting to wire them up. The relationship shipped in #49 (synced snapshots) and was **removed entirely in #50**; `experience` V6 drops the never-populated V5 table. | Built + merged (catalog); relationship unwired by decision |
| `audit` | Platform-wide **append-only audit trail** (#54). Every operator-facing mutation (29 use cases wholesale) appends via `shared/port/AuditTrailPort` **inside the business tx** — "no unaudited mutation" (entry rolls back with the action); S3-backed mutations append after the write instead. `actor_name` frozen at write (filter-only, not sortable); `request_id` via the correlation filter; actors USER/SYSTEM (SYSTEM has no emitter yet, by decision). Read: `GET .../audit-log` (cursor list) + `/{entryId}`, member-visible. | Built + merged |
| `reference` (languages) | + `reference.languages` master list (`GET /api/languages`, seeded en/es/fr/it/pt/de) — the content-language allowlist operators validate against. | Built + merged |
| `experience` | The operator's sellable product. CRUD + publish/unpublish under `/api/tour-operators/{id}/experiences`; member-read/admin-write; media validated-on-write (`MediaAssetBatchQuery`) and resolved-on-read; handle per-operator unique and **immutable**; `published` and `featured` booleans. **Per-locale translations**: nullable overlay fields + an optional localized handle, validated against `OperatorLocalesQuery`. **Canonical and localized handles are one namespace** (PATTERNS §4d) — the derived canonical handle suffixes past both, an explicit localized handle 409s against both. **`starting_price`** (`NUMERIC(12,2) NOT NULL DEFAULT 0`) is the storefront's "from" figure, operator-set since #119; the admin does not send it yet. **SEO overrides** on the experience and its overlay, authored through the existing create/update and translation-upsert paths — no use case of their own. It implements `StorefrontExperienceQuery`: `findFeatured` for the globals' cards and `findPublishedHandles` for menu links, both in the rendered locale. **What is absent is a read *by handle*** — this port has no `findByHandle` where `StorefrontPageQuery` does, and the experiences listing is still a placeholder, so nothing resolves an experience handle to a page and a §4d shadowing handle stays unobservable until the detail page lands. Slots and pricing are the row below; a pickup link and delete are still out. | Built + merged |
| `media` | Operator media library. Upload (multipart, ADMIN+) / list (cursor, member) / get (member) / **describe** (`PATCH .../media/{id}`, ADMIN+) / delete (ADMIN+) under `/api/tour-operators/{id}/media`. Member-read / admin-write; storage_key only (URLs resolved at read); allowlist images+PDF ≤25MB (SVG excluded by decision — script injection); ships the **`MediaAssetBatchQuery`** cross-context seam (renamed from `MediaKeyBatchQuery` in #100 when it began carrying alt and dimensions rather than a bare key), consumed by `UpdateBrandUseCase`, experience's `MediaReferenceValidator` and `shared.media.MediaUrlBatchResolver`. **Dimensions are measured at upload (#109)** through `ImageDimensionsPort` — a port because `javax.imageio` is not `java.*` — reading the header only and answering empty for a PDF, an unknown format or a damaged header, so it can never fail an upload. **`alt` is written afterwards**, since only the uploader knows it. Rows predating #109 keep null dimensions; nothing backfills. | Built + merged |
| `page` | **CMS content pages** (#56) — merchant-authored About/Contact/FAQ/policy content, served at `/pages/{handle}`. Title + an **operator-chosen handle** (unique per operator; collision = **409, never auto-suffixed** — it is the permanent URL), raw-HTML body (≤256 KiB, stored verbatim, NUL rejected — escaping is a render concern), SEO overrides (Shopify's admin limits), a `published` **boolean** (**no scheduling**, by decision) — it was a DRAFT/PUBLISHED status enum until #142 swapped it for experiences' shape, on the wire as well as in the column, because the storefront asks both the same question and a menu item can point at either. **Rename is its own endpoint** — changing a permanent URL is a deliberate act. Per-locale translations mirror experience's (nullable overlay + localized-handle rules: explicit → 409, derived-with-probing, absent → canonical serves the locale). 12 endpoints (8 + 4 translation); audited from day one. Ships `PageOwnershipQuery` **and `StorefrontPageQuery`** (`findPublishedHandles` for menu links, `findByHandle` for the page itself, both in the rendered locale), implemented in `infrastructure/query`. That seam was deleted 2026-08-02 with the serving side and **rebuilt**; `/pages/{handle}` and `/{locale}/pages/{handle}` are live routes on `StorefrontCmsPageController`, not a plan. | Built + merged |
| `metafield` | **Custom data — the operator's own schema**, both halves in one context (metaobjects did not earn their own). **Metafields**: namespace.key definitions per (operator, owner type) over an 8-type catalogue using **our own type codes**, not Shopify's `*_field` forms; ownerType/namespace/key/type are **immutable** and delete cascades values. Values are **owner-generic** — `MetafieldOwnerType {EXPERIENCE, PAGE, TOUR_OPERATOR}`, reaching owners through `ExperienceOwnershipQuery`/`PageOwnershipQuery`. `TOUR_OPERATOR` needs no seam at all: the owner *is* the tenant, so `ensureOwned` is an equality, and its mount carries **no owner id in the path**. **The price of owner-generic is a bare `owner_id` with no FK**, so the database cannot cascade: **any owner that can be deleted must call `MetafieldValueCleanup` inside its delete transaction** (`page` does; experiences have no delete yet). **Metaobjects**: custom content types — definitions with ordered, renameable fields (the last one is not removable) + entries with an operator-chosen handle, row-per-field values and a `published` boolean. **`metaobject_reference` wires them together**: a reference-typed definition pins one metaobject type, values validate type+ownership, deleting a referenced entry **clears the pointing values in the same tx**, and a pinned type cannot be deleted while targeted (409). 45 endpoints, audited day-one. **The line to hold: a metafield is content the storefront renders and nothing else understands.** Anything with behaviour — hours that decide bookability — needs real columns and domain rules; "just make it a metafield" is how a schema quietly stops being able to answer questions. | Built + merged |
| `contact` | **Contact-form inbox — the admin half only, by decision** (#63). `contact_messages`: UUIDv7 id so reverse-chron *is* `-id`, bare `tour_operator_id`, **required name** (nullable by mistake until contact/V3 — an inbox a person reads and replies to is worse off with an address and no name), email, summary, verbatim TEXT content. The row is immutable: read-state was dropped in #130 and `ContactMessageRepository.save` went with it. 3 endpoints — cursor list, get, delete (ADMIN+, audited with the **summary only**, so the sender's email stays out of the trail). **Intake is deleted and the inbox is a mailbox nothing can fill.** `SubmitContactMessageUseCase` was the only creator; `ContactMessage.submit` and the four validation value objects are **parked deliberately rather than deleted**, because the intake returns and the domain is what makes that cheap. What it established stays true for then: throttled per tenant 30/h **before** validation, so rejected input costs a token too, and **not audited by decision** — the row is its own record, so a `STOREFRONT` audit actor is still unneeded. | Built + merged |
| ~~`rendering`~~ | **Deleted 2026-08-02** — the whole serving side, when the storefront ran as a separate renderer calling in over HTTP. Every decision it embodied was a consequence of that shape, which is why none of it was ported forward. Recover from git if a piece turns out to fit. | Deleted |
| `storefront` | The public site, multi-tenant, resolved from the request host. **Currently a placeholder serving JSON, not HTML** — `/` and `/{locale}` serve the real globals contract (`tourOperator`, `localization`, `routes`, `linklists`, `featuredExperiences`, page SEO), `/pages/{handle}` serves globals + `page`, and four addresses still answer `{"handle","status"}`. The password gate is live and a new store is private by default. **The full record — the contract, the render path and what is still open — is `backend/PATTERNS.md` §2a and §2b.** It is kept there and not here: the storefront is backend design, and it was previously written out in three places. | Placeholder; contract settled |
| `shared` | Kernel: security layer, exceptions, Kafka producer, Redis rate limiter, cross-context ports, media URL resolver, Flyway-per-schema. | Built |

### The rules that shape every change

- **A context never imports another context.** Two channels only: a **shared query port** (`shared.port.<Noun>Query` + a `<Noun>View` of primitives, implemented by the owning context in its `infrastructure/query`) or a **Kafka event**. `shared` and `reference` are the shared kernels everyone may import.
- **Use cases are plain POJOs**, with no Spring annotations. Each is hand-wired as a `@Bean` in its context's `infrastructure/config/<Ctx>UseCaseConfig`.

  **ArchUnit enforces an allowlist.** The application layer may depend on `com.vointika..` and `java..` and **nothing else** — no third-party library at all, not even a logging facade. Reaching for one means a port is missing.

  **`java..` does not cover `javax..`.** `javax.crypto` fails the rule exactly like a third-party jar. That is why the storefront's unlock-cookie HMAC is `UnlockTokenPort` plus an adapter, not a policy class (PATTERNS §8d).

  A best-effort side effect — a storage delete, a broker publish — logs in the adapter that fails. A use case with something of its own to report uses `DiagnosticLogPort`.
- **Every operator-facing mutation appends to the audit trail inside the same transaction** as the mutation (PATTERNS §8b). No unaudited mutation.
- **Any list that grows with business activity uses the shared list framework** — keyset cursor, typed filters, `ListSchema` (PATTERNS §4b). Members, bookings, orders, audit. Never return an unbounded array for one of those; that mistake has already been made and fixed once. **Tenant-scoped is not by itself the test** — twelve tenant-scoped endpoints return a bare `List<>` today because a closed set does not page (the `/translations` reads are capped by the operator's enabled locales). `/metafields` is the one to watch: it is bounded only by how many definitions the operator creates.
- **Our own asset URLs are never stored.** Store a bucket-relative storage key, resolve to a URL at read time, so changing the bucket or domain needs no data migration (PATTERNS §5). This is about *our* objects: a URL the operator typed at an address we do not host — a menu's `EXTERNAL_URL`, a brand social link — is stored as given, because there is no key to store instead.
- **Responses identify themselves with `id` + `context`** — never `userId`/`tourOperatorId`, and the discriminator is **not** called `type` (PATTERNS §4a). The ban is on *that* use of the name, not the name. `MetaobjectDefinitionResponse` is the illustration: it carries `id` + `context:"metaobject-definitions"` per the rule, **and** a `type` naming the metaobject's own kind, **and** a nested `FieldResponse.type` holding a field's data type. Three `type`s, none of them the discriminator, all fine.

### One API surface, one auth model

**The admin/operator API** — JWT bearer tokens. There is no `/api/storefront/**` surface and **no shared secret anywhere**: the storefront does not call in over HTTP, it renders in-process. Tenant-scoped routes live under `/api/tour-operators/{tourOperatorId}/**` and are gated in **two layers**: a membership interceptor (non-member → **404**, byte-identical to a missing operator) plus per-use-case role gates (`ensureAdmin`/`ensureOwner`) through the `TourOperatorMembershipCheck` port. Authorization belongs in the use case, not only the router — the router's matching is looser than the id binder, which is how an IDOR got in once.

**The storefront serves its globals as JSON, and nothing about it is authenticated.** A locked store is gated by the storefront password, which is an interceptor and not a login.

**The contract is `PATTERNS.md` §2a and the render path is §2b. That is the whole record; nowhere else carries a copy.** What belongs here is the shape of the code.

`storefront` resolves the tenant from the host and owns **nine addresses**. Eight are pages: `/`, `/{locale}`, `/experiences`, `/{locale}/experiences`, `/policies/{type}`, `/{locale}/policies/{type}`, `/pages/{handle}`, `/{locale}/pages/{handle}`. Four of those serve real data — `/`, `/{locale}` and both `/pages/{handle}` forms; the other four answer `{"handle","status"}`. The ninth is **`/password`**, the gate — `GET` renders it, `POST` submits it, and it is a route like any other, which is why `PasswordPageController` is one of the four controllers below.

There are **four controllers**: `StorefrontHomeController`, `StorefrontCmsPageController`, `StorefrontPlaceholderController`, `PasswordPageController`. The tenant seam is `TenantHandleResolver` plus `StorefrontTourOperatorQuery`. There is no `StorefrontTenantQuery` — it was never rebuilt under that name. The password gate is back in full.

An unknown handle and an unpublished locale both answer 404 in the application's usual error shape. They are deliberately indistinguishable.

**It answers JSON, not HTML, on purpose.** The placeholder period is for getting the *data* right before themes exist, and markup nobody reads hides a wrong field. Mustache stays for the same reason: the dependency and `StorefrontMustacheConfig`'s compiler are the render-path decision waiting for themes, and its settings test keeps the traps in `STACK.md` true meanwhile.

Routes stay unauthenticated through the same `PublicRouteRegistrar` every other public route uses, not a host-matched `SecurityFilterChain`. Nothing needs storefront requests to have *different* security, only *no* authentication.

**A new store is private by default.** `CreateTourOperatorUseCase` generates a storefront password and sets `password_enabled` at creation. So a new operator's store exists at its address and answers the gate rather than the shop. That is Shopify's model: a new store stays password-protected until the merchant is ready. Existing operators were left open. The operator reads the password back through `GET /api/tour-operators/{id}/storefront-password`, and disables the gate when it wants to sell.

**The gate runs before locale resolution.** This is the rule that cost the most to learn.

Resolve the locale first and a locked store 404s an unpublished locale while redirecting a published one. That tells an anonymous visitor the store exists and which locales it has. The guard is `StorefrontHomeControllerTest.aLockedStoreRedirectsEvenForALocaleItDoesNotPublish`. It is also why the gate landed *after* the index slice: it needs `LocaleRule` to order against.

A gated store still answers on its address, with the gate page. It is not a 404 — Shopify leaks existence the same way.

The unlock cookie is `HMAC-SHA256(key = storefront_password, message = operatorId)`. Rotating the password therefore invalidates every outstanding cookie for free.

Public (unauthenticated) routes are opt-in per context via the `PublicRouteRegistrar` SPI; rate-limit rules likewise via `RateLimitRuleRegistrar`. Never add either to a central hardcoded map.

**A `PublicRoute` pattern is a security pattern before it is a route, so an unconstrained path variable is a hole.** `/{locale}` reads like one page route. It actually `permitAll`s **every single-segment path in the application** — `/error` today, and whatever `/health` or `/metrics` lands later, silently.

Constrain the variable, and define the pattern **once** (`StorefrontRoutes.LOCALE`) for the mapping, the `PublicRoute` entries and any interceptor patterns. The group must be non-capturing: `PathPatternParser` rejects capture groups outright (PATTERNS §11).

**A `PublicRoute` matches one HTTP method, so a page route needs `GET` *and* `HEAD`.** Spring MVC serves HEAD from a `@GetMapping` for free. Spring Security does not. A GET-only entry rejects HEAD at the filter chain as a **401 in the JSON error shape**, never reaching MVC.

That is harmless on a JSON API nobody HEADs and wrong on a public page: crawlers, link checkers, uptime monitors and CDNs all send HEAD. `storefront` shipped without it because every test and curl used GET. It took a request against the built stack to find.

`servesHeadAsWellAsGet` pins it, in three tests covering **seven of the nine** addresses — the entry is per route as well as per method, so coverage is per route too. `StorefrontPlaceholderControllerTest.EVERY_ADDRESS` is named wider than it reaches: it walks the four placeholder addresses, and the home and CMS tests add three more. **`/{locale}/pages/{handle}` and `/password` have no HEAD test.** Both are registered correctly — `StorefrontPublicRoutes` loops `PAGE_ROUTES` adding GET+HEAD per route, then lists the gate's three methods explicitly — so nothing is broken today. That loop is why registration is not where the risk is any more, and the tests are: a route added to `PAGE_ROUTES` is registered on both methods by construction, while its HEAD test is still written by hand or not at all.

Counting the `@GetMapping`, **a page route is registered in three places** — four while the password gate exists, since its interceptor needs every page pattern too. Define the pattern once in `application/policy` (PATTERNS §11).

### Migrations

Flyway runs **once per domain**, each into its own Postgres schema, in the order listed in `shared/infrastructure/flyway/FlywayPerDomainConfig.DOMAINS` — the order matters wherever cross-schema FKs exist. A new context adds its folder `src/main/resources/db/migration/<ctx>/` **and** an entry in `DOMAINS`. **Never modify an applied migration**; add the next `V`. Curated reference/seed data lives in the migration itself; dev-only fixtures live in `docker/dev-seed/`.

Renames must sweep beyond `src/` — the dev seed and docs reference table and column names too.

### Tests

Unit tests (JUnit 5 + Mockito, no Spring) for value objects, entity behavior, and use cases. Controller tests are **RestDocs documentation tests** — `@WebMvcTest` + `@Import(SecurityConfig.class)`, asserting behavior *and* emitting the snippets that build the API guide. Four recurring traps:

1. A `@WebMvcTest` whose context loads `WebConfig` needs a `@MockitoBean TourOperatorMembershipCheck`, or every request 500s.
2. **Any storefront `@WebMvcTest` needs a `@MockitoBean CheckStorefrontLockUseCase`**, for the same reason. `StorefrontWebConfig` is a `WebMvcConfigurer`, so every slice registers the gate's interceptor, which resolves that use case per request.
3. A public-route test that omits its `PublicRouteRegistrar` 401s everything. The assertions then pass without testing anything.
4. Mockito's, and it bit three times in one day: a test helper that stubs a mock must be called *before* the `when(...)` it feeds, never inside `thenReturn(helper())`. Mockito reads the nested `when()` as unfinished and fails with `UnfinishedStubbing`, pointing at the helper rather than at the caller.

## Conventions

The working rules are LAW: §2.4 never over-engineer · §3 the landing ritual · §4 never assume · §6 craft (comments, commits, dead code). Only the calibration for this repo lives here.

- **Javadoc runs heavier than LAW §6.1's default, deliberately.** A hexagonal context has real seams. The Javadoc on a port, a security filter or a migration is often the only place a decision is recorded — `EndpointRateLimitFilter`'s note on why the counter keys on the matched *pattern* rather than the concrete URI is load-bearing. The rule still bites, though: `/** Returns the user. */` over `getUser()` is noise. Keep the why, the trap and the rejected alternative. Delete the restatement.
- **Dead code has no mechanical gate here.** Java offers no `noUnusedLocals` equivalent, so LAW §6.3 is a look. **ArchUnit** takes whatever is automatable — it already fences the §2 boundaries, and it is the right home for a port nobody implements or a use case no `@Bean` wires.
- **Probing has two traps that cost real time.** **Never probe by writing into
  `src/main/resources/db/migration/`** — `contextLoads` boots the real application, so
  Flyway *applies* whatever is sitting there to the dev database. A throwaway migration
  becomes a permanent `flyway_schema_history` row and its DDL really runs; it surfaces
  later as a checksum mismatch. Probe against a scratch copy instead. And **deleting a
  file under `src/main/resources` does not remove it from the build** — Maven copies
  resources into `target/classes` and never prunes, so Flyway and every other classpath
  reader still see the stale copy.
- **Confirm a mutation landed before believing the result.** A `sed` that matches
  nothing leaves the file untouched and the suite green, and the conclusion is "the rule
  has a hole" or "that test is fake". Both are wrong, drawn from a command that silently
  did nothing. One `grep -c` between the edit and the run removes the whole class of
  error. A guard that passes vacuously is worse than no guard.
- **A commit body is rarer than it looks** (LAW §6.2). The durable *why* belongs in the doc that owns the rule — `PATTERNS.md` for a recipe, *Open work* below for a decision; the reviewer's context belongs in the PR description; the diff belongs in git. A message that repeats all three is paying three times.

## Open work

Carried forward when `MAP.md` was deleted (2026-08-20). Debt is owed; Backlog is
wanted but unscheduled; the standing decisions below are settled and are not to be
re-litigated without a reason.

### Debt

- **No way to search a list, so the admin drains it** (2026-08-14, found by the
  frontend audit) — `ListConstants.PAGE_SIZE` is a fixed **20** and `ListQuery` has
  no limit field, so a picker that needs every row walks the cursor. An operator
  with 300 experiences waits on 15 chained requests before the availability dialog
  is usable, and four call sites do this (experiences ×2, pages, metaobject
  entries). The fix is a server-side search/typeahead endpoint. It belongs with the
  **body-returning endpoints that document no field table**, which is **closed as of
  2026-08-17**: the count went 20 → 0 across the API-docs series, `metafield` taking
  the last one. The scan that keeps it true is in `backend/PATTERNS.md` §9a. The
  frontend's own three findings from that audit stay in its repo.

- **The §4d cross-namespace guard is a pre-check, not a constraint** (2026-08-01,
  from the review of the `experience` fix) — affects `page` and `experience`
  equally. Uniqueness *within* a namespace has a unique index behind it, so a lost
  race is rejected; nothing spans `experiences.handle` and
  `experience_translations.handle` (or the page pair), and nothing can without a
  trigger. Two concurrent writes, one per namespace, can still land on the same
  value and shadow. Narrow window, no known occurrence, and the alternatives
  (trigger, or a shared handle table) are both larger than the exposure — recorded
  so the guards are not mistaken for completeness. PATTERNS §4d now says so.
- **`experience` upsert has no named 409 on a lost handle race** (2026-08-01, from
  the §4d fix). `page`'s translation upsert wraps its transaction in
  `catch (UniqueConstraintViolationException)` to name the conflict. Experience's
  does not, despite having the same partial unique index
  (`uq_experience_translations_operator_locale_handle`). **Cosmetic, not a status
  bug**: uncaught, the exception already maps to 409, so the only difference is
  the message ("A concurrent write already created this record" vs an
  operation-specific one). Left out of the §4d fix under LAW §6.4 rather than
  widened into it.
- **A slot date-range filter is not expressible** (2026-07-31, from the `shared`
  audit) — `ListSlotsUseCase` makes `startAt` **sortable but not filterable**, and
  `ListSchema` has no builder for `LocalDateTime` — the filter kinds are `text`,
  `set`, `number`, `bool`, `time`→`LocalTime` and `instant`→`Instant`, and the
  unused `date`→`LocalDate` one was deleted.
  So "slots in August" cannot be asked for — the single most obvious filter on a
  departures list. Needs a `localDateTime(...)` filter kind wired to the existing
  comparable predicate, which `ValueCoercion` already parses. Add it when a
  screen asks; do not re-add a filter kind nothing declares. It used to be half
  of a pair with the contact unread filter — **that half was dissolved in #130**
  by deleting read-state, so a null operator on the shared framework now has no
  caller asking for it.
- **The per-resource metafields read is an unbounded array** (2026-07-31, from the
  `metafield` audit; **scope widened 2026-08-15**) — **three endpoints**, not the two
  this entry named until the audit recounted them: `GET .../experiences/{id}/metafields`,
  `.../pages/{id}/metafields`, and `.../tour-operators/{id}/metafields`, the last added
  with the `TOUR_OPERATOR` owner type in #139. Each returns a bare
  `List<MetafieldValueResponse>` while the other three lists in the context all use
  the shared cursor framework. Its size is bounded only by how many definitions the
  operator has created for that owner type, and definitions are themselves
  cursor-paginated, so the bound is "whatever the operator built". Not fixed in the
  audit because it is a **wire-contract decision**, not a defect: the admin editor
  overlays every value onto the definitions form in one render, so paginating it
  makes the editor page. Either cap + paginate it, or record the exemption the way
  the reference lists have one.

*Audited 2026-07-21: no TODO/FIXME/HACK/stub markers, no orphan fallback code, no
dead code, no hidden `@SuppressWarnings` hacks (the Kafka raw-type ones are the
deliberate Boot-4 injection). Reference/ui-language plain-array lists are curated
& bounded — intentionally exempt from §4b, not debt.*

### Backlog

Known wants, not yet scheduled — deliberate future work, not shortcuts.

Known wants, not yet scheduled (deliberate future work, not shortcuts):

- **Where per-page-type SEO text lives** (2026-08-03, #92 — **was filed as Debt
  until 2026-08-11**) — every storefront page falls back to the operator's
  `seo_title`/`seo_description`, and the experiences listing has no entity of its
  own to carry one. The home page is fine (the shop *is* its subject); a listing
  sharing the shop's title is already duplicate-ish, and every further page type —
  a collection, a search — inherits it. **Nothing was invented for it in #92 on
  purpose: a schema decided by a template is the wrong order.** The options are a
  `page`-like row per page type, theme settings, or nothing. **Settle it before
  the experience detail page ships its own answer**, which is the first thing
  that would force one — so it is parked with the storefront, and unparks with
  it.
- **Should a storefront card show that an experience is featured?** (2026-08-03,
  #92. **Filed as Debt until 2026-08-11, where it did not belong.**) `featured`
  already orders the listing, featured rows first. The page just says nothing about
  it, so a visitor cannot tell which ones the operator promoted.
  Nothing is owed and nothing is half-built: the card component that carried the
  flag was **dropped in review** under LAW §2.4 rather than parked in a shared
  port waiting for an answer, precisely because nothing read it. What is open is
  a **product question**, and the field it would need is one boolean added the
  day a badge renders. Parked with the storefront either way.

- **Experience `type` and `category`** (2026-08-12, decided by the user during
  the `shop`-object comparison — **both are wanted, neither is scheduled**). An
  experience carries no classification at all today: `tags` was dropped in V10,
  and `audience` is who a slot is priced for, not what kind of thing an
  experience is. That is why `shop.types` was filed as "nothing to expose"
  rather than as a contract gap — there is no column behind it.
  **They are different in kind, which is why Shopify keeps both.**
  `product.type` is **free text the merchant types** — flat, unvalidated,
  per-store, and it drifts ("Boat tour" / "boat tours" / "Boat Tours" become
  three). `product.category` is a node in a **published taxonomy**:
  `taxonomy_category` carries a hierarchical id (`hb-1-9-6`), a **localized**
  `name`, and `ancestors` for a breadcrumb. There is deliberately **no
  `shop.categories`** — the taxonomy is the same for every store, so only the
  per-store invention is worth enumerating.
  **So they are two slices, not one.** `type` is a nullable column on
  `experience` plus one `SELECT DISTINCT` behind `tourOperator.types`. `category` is a
  `reference` slice first — a curated tree with stable ids and localized names —
  and only then a FK on `experience`. Same line the address slice drew between
  country (closed set → reference table) and city (open set → free text on the
  row). Neither blocks the storefront contract; both change it when they land.

- **Member notification-subscriptions** — `/me/notification-subscriptions`,
  personal per-alert-type prefs (was briefly "next slice" before the booking
  loop took priority).
- **List-my-pending invitations** — a `/me` view of invitations addressed to
  the caller, cross-operator (SPA onboarding; distinct from the operator-scoped
  list already shipped).
- **Handle history + 301s** — renaming an experience handle or a page handle makes the
  old URL 404. Deferred once at #36 and again at #56, and the storefront has now
  made it externally visible: shared links and indexed results break silently.
  Needs a history table and a redirect on miss.
- **Metafield v2** — per-definition validations and choice lists, plus the
  `list.*` and `rich_text` types. Scoped while #57–#60 were built and never
  scheduled; the operator-level owner type and storefront read access were part of
  it and have both since shipped.
- **Locale `is_published`** — `tour_operator_locales` shipped without it (#32),
  deferring the question "to the storefront". The storefront exists now and treats
  every *supported* locale as published, so the idea is either dead or a real gap:
  today an operator cannot prepare a translation without exposing it. Decide.
- **Structured JSON logging** — today logging is plain text → stdout with MDC
  correlation (`requestId`/`userId`, propagated across the Kafka hop). For prod
  log aggregation, add a JSON encoder (e.g. a `logback-spring.xml` with an
  ECS/logstash encoder) behind a profile, preserving the correlation fields; keep
  dev human-readable. Non-urgent.
- **Country flag assets** — `country.flag_key` exists (**nullable since reference/V6**, `flags/{iso2}.svg`)
  and resolves to `flagUrl`. **The bucket question is settled and three flags
  exist**: ES/US/DO ship in `docker/dev-seed/flags/` and `minio-init` uploads them
  to `avatars/flags/`, the same public base media uses (#146). What is left is the
  other 246 — a full SVG set, a production upload path (`minio-init` is dev only),
  and then one `UPDATE reference.country SET flag_key = 'flags/' || lower(code) ||
  '.svg'` plus a re-tightening `SET NOT NULL`. Still its own small slice.

### Decided

- **Repo shape** (2026-07-19) — **separate repo per project**: backend, admin,
  storefront (and themes) each own their own git repo, as before. *Not* a
  mono-repo. LAW therefore lives at `/home/jefrycayo/vointika/` root — the
  cross-repo spine, mirroring the archived `CONTEXT.md` precedent. **Amended
  2026-07-31:** that root is itself a git repo (the three project repos are
  `.gitignore`d, so the separate-repo-per-project decision is untouched). It had no
  history, no diff and no backup, and LAW §0.4 requires an amendment to be
  accounted for "in the commit" — impossible while nothing tracked it. **Amended
  2026-08-20:** it tracked LAW *and* MAP until MAP was deleted; per-repo state now
  lives in each repo's own `CLAUDE.md`, which is what the section above is.
- **Events on Kafka** (2026-07-19) — the `EventPublisherPort` seam is backed by a
  Kafka producer; contexts produce/consume over Kafka, never direct imports.
  **Topic convention:** `<producing-context>.<event-kebab>`, one topic per event,
  keyed by recipient email. Kafka client fenced to `shared.infrastructure.kafka` +
  the notification consumer (ArchUnit).
- **The dev stack's shape** (2026-07-19) — Kafka is KRaft single-node, no
  Zookeeper. SES runs against **`aws-ses-v2-local`**. LocalStack was rejected: its SESv2
  `SendEmail` retrieval is broken (`/_aws/ses` KeyError 'Source') and paywalled.
  Reading the sent mail is the whole point, since the verify and reset tokens leave
  only by email. Versions and endpoints are in `backend/STACK.md`. Without AWS
  credentials the async send logs a failure and the flow still completes.
- **Email locale rides the event** (2026-07-20) — identity emails are sent in the
  recipient's language. Each identity event carries a `locale` (set from
  `User.language` at publish); the notification consumers pass it to
  `SendNotificationUseCase` (exact→subtag→en fallback). notification never queries
  identity. Templates ship per-locale on the classpath (en + es), tracking
  `app.identity.ui-languages`. **Register captures the language** as an optional
  **body field** (`POST /register {language}` — the FE's Paraglide locale),
  validated against the allowlist, stored on the new user; unsupported/absent →
  `en` (never fails a signup). Not a header — only register captures; it's a
  persisted attribute.
- **UI languages are not reference data** (2026-07-20) — a UI language is a
  locale code, not a row with attributes. Model = `User.language` (the choice) +
  `app.identity.ui-languages` config allowlist (validation) + **authenticated**
  `GET /api/ui-languages` (the picker's single source of truth; codes only,
  labels via `Intl.DisplayNames`). **`reference.languages` is a different list** —
  it is the *content*-language allowlist an operator publishes in, not the admin
  UI's. Grow a
  language = add the yml code + ship the FE catalog + **ship the email templates
  and add the code to `ClasspathTemplateCatalog.LOCALES`**; no migration.
  **Corrected 2026-08-01** (the `notification` audit): this entry, PATTERNS §8 and
  two Javadocs all said "zero code", and email was the exception nobody had
  counted — a language on the allowlist with no templates sends in English rather
  than failing. `TemplateLocalesTrackUiLanguagesTest` now fails the build on
  divergence.

- **A column lands with the feature that reads it** (2026-07-20) — the rule the
  `tour_operators` row was created under, and the reason it still has **no `status`
  or fee field**: a lifecycle column arrives with whatever reasons about it. The row
  has grown a great deal since (brand, locales, the password gate, a structured
  address) and every one of those came with its reader.
  **The welcome email is sent through the standard path** — event → notification
  consumer → SES, in the creator's UI language — which is why a `touroperator` event
  exists at all; it was deferred while nothing consumed it. Recipient
  email/name/language is resolved at publish time via `UserAccountQuery.findContact`
  and rides the event, so the consumer never queries identity.
  A generic `TourOperatorCreated` event for storefront initialization is still **not**
  built: the storefront context exists now, but nothing in it needs to react to a
  new operator.

- **What an experience's "from" price means** (2026-08-01) — `starting_price`
  (experience/V7) sits on the row the card already loads, because rendering may not
  fan out to per-slot prices. Two rules are decided: **nothing scheduled → 0**, and
  the figure is **per person**, not per `paxPerUnit`. Free tiers are excluded, which
  is what makes 0 safe rather than a lie — a real starting price is never 0, so 0
  means "nothing priced yet" and the card hides the badge. **Which slots count is
  still open**: `slots.status` is AVAILABLE/SOLD_OUT/CANCELLED and departures are
  dated, so a naive MIN would quote a cancelled or past one. It is operator-set
  since #119, so nothing derives it today. The next departure is a separate
  question and stays out.
- **Invitation model** (2026-07-20) — invitations key on **email** (invitee may
  have no account). Raw token only in the emailed link; **SHA-256 hash at rest**
  (`token_hash` unique). 7-day expiry judged **lazily** on access (no job). At most
  one PENDING per (operator, email) — DB partial unique index. Invite = ADMIN+;
  invitable roles = ADMIN/STAFF only (**OWNER is transfer-only**, 422). Accept is
  **public** (token is the capability); anonymous accept provisions a
  **verified-from-birth** user via `InvitedUserProvisioning` + issues a session
  (auto-login), and **fails closed 409** if the email already has an account
  (pre-registered-email attack). Invite email sent in the **inviter's** UI language
  (invitee has none yet). **All four invitation actions are audited**, entity type
  `INVITATION` — `member.invited` (not `invitation.created`; the invite is a team
  action), `invitation.accepted`, `invitation.resent`, `invitation.revoked`. This
  entry said audit was "subtracted (no audit context yet)", written six days before
  #54 built one and swept these in with the other 28.

- **An operator may change their timezone, and departures keep their wall-clock
  hour** (2026-08-08, #108) — **asked for by clients**, so it is a requirement
  rather than a tolerated side effect.
  Slots store operator-LOCAL wall-clock times (`LocalDateTime`, no zone), so
  changing the zone leaves every stored departure at the hour it was authored: a
  10:00 sailing stays a 10:00 sailing, and only its absolute instant moves.
  **That is the right behaviour for the case that actually happens** — an
  operator correcting a timezone they set wrongly, or one whose business moved.
  Their tours always ran at 10am local; the zone was the thing that was wrong.
  Rewriting the rows to preserve the absolute instant would do the opposite,
  moving every departure to a time nobody scheduled.
  So **nothing migrates the slots, by design**, and this is deliberately not
  carried as debt. Two things follow, recorded so they are not rediscovered as
  bugs: `CreateSlotUseCase`'s past-date guard starts judging against a different
  today, and the change is audited with the before/after zone. Revisit only if
  bookings ever need the absolute instant preserved for an already-sold
  departure — which cannot arise yet, since the transaction half is unbuilt.

### Open decisions

Coordination-critical and unresolved. Record the outcome under *Decided* when closed.

Coordination-critical and unresolved. Resolve deliberately; record the outcome
in **Decided** above when closed.

1. **Context collapse.** Which of the old ~19 contexts survive, merge, or die.
   **Still to place: cart · theme · payment · sales** — and none of them can be
   settled before the transaction arc exists, so this decision is only as open as
   that work is unbuilt.

   Thirteen are placed and built: identity · notification · reference ·
   touroperator · media · experience · audience · pickup · audit · page ·
   metafield · contact · storefront, on the `shared` kernel. Three of those
   placements were close calls and are the precedent for the four remaining:
   **metaobjects did not earn a context.** They live inside `metafield` as one
   custom-data context rather than becoming a fifteenth. **`menus` and the
   storefront password went into `touroperator`**, because one storefront per
   operator makes the operator the scope. And **`storefront` did earn one**
   (2026-08-03), on lifecycle: it is public, unauthenticated and eventually HTML,
   against `touroperator`'s authenticated JSON admin surface. One context owning
   both audiences is the thing to avoid. The decisive point there was that the
   ArchUnit fence is the *feature*: `storefront` cannot import `touroperator`,
   which forces the read seam.
2. **Shopify OS 2.0 scope.** Ground in fresh research before any theme work
   (decision from the rebuild conversation). It was parked as downstream of the
   render path — **that block is gone**: the render path is decided (Mustache,
   in-process). So this is no longer blocked, it is simply not started, and it is
   the first thing to settle when theme work begins. What it has to answer is
   bounded by what Mustache can express — logic-less templates, and no custom
   tags, which forces a section's schema out of its template. **That constraint,
   and what is still open under it, is `backend/PATTERNS.md` §2b.**

3. **Where a platform-level security event goes.** A refresh-token *reuse* — a
   replayed refresh token, i.e. a probable theft — is detected in
   `RefreshAccessTokenUseCase`, revokes the whole token family
   (`revokeAllByFamilyId`), answers 401, and is reported **only** through
   `diagnosticLog.warn(...)` → SLF4J. Verified 2026-08-11: no `AuditTrailPort` in
   that class, and nothing in `src/main` calls `AuditActor.system()`.

   **This sat in Debt until 2026-08-11 and did not belong there.** Nothing is
   owed: detection works, revocation works, the attacker is locked out. The
   security *control* is complete. What is missing is the ability to answer a
   question afterwards — "was anyone's session stolen last month?" — and there is
   no asker, because nothing is deployed. It also has no action behind it, only a
   question, which is the tell: every real Debt entry names a thing to do.

   **The tenant audit trail cannot take it as it stands.**
   `audit_log.tour_operator_id` is NOT NULL and `NewAuditEntry` requires it; this
   event happens in `identity` and has **no** tour operator (a user may belong to
   zero, one or several). The surface is also tenant-scoped by construction:
   `ListAuditLogUseCase.SCHEMA` is `.tenantScoped()`, and the controller sits under
   `/api/tour-operators/{id}/audit-log` behind the membership gate. So an invented
   tenant would file a platform security event in one customer's activity feed,
   readable by their staff and invisible to us.

   Note the rule does **not** force this: §8b audits every *operator-facing*
   mutation, and revoking a token family is not one.

   **Not forced until there is a deployment with real users** — which is also
   when the cheapest answer might turn out to be "the host already retains and
   searches the app log," costing no code at all. The alternative, if it is not,
   is a non-tenant home for security events; **what must not happen is making
   `tour_operator_id` nullable because a backlog list said to.**

## Working rhythm

Trunk is `main`; no direct commits. Every slice gets a short-lived branch (`feat/…`, `fix/…`, `chore/…`, `docs/…`) → PR → **merge only when the user says so**. A slice is done when the full suite is green *and* the change has been verified live against the running stack.

End any session that changed something with the landing ritual (LAW §3): make this file, `PATTERNS.md`, `STACK.md` and any saved memory true. A version trap goes in `STACK.md`, a pattern that has now repeated twice in `PATTERNS.md`, a decision or a piece of owed work in *Open work* above, and the per-context row in *What each context owns*. If a fact moved, delete the old copy rather than leave it to drift.

**This file is the only reason the next session knows where things stand.** It took that job from `MAP.md`, deleted 2026-08-20 — so a claim here about what is built, owed or decided is load-bearing in a way the rest of the file is not, and goes stale the same way MAP's did. Grep the claim, not the file you happen to be reading.
