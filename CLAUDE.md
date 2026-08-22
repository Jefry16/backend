# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read these first

**Nothing loads these for you.** A handful of javadoc comments name `PATTERNS.md` and `STACK.md`, so the code knows they exist — but no import pulls one in and no build step reads one. Load them yourself:

| Document | Location | What it is |
|---|---|---|
| **LAW** | `../CONSTITUTION.md` (the parent directory — its own git repo) | The rules. Short, read whole, every session. |
| **PATTERNS** | `PATTERNS.md` (in repo) | The recipes. Before building anything, find the matching one — don't reverse-engineer existing code. |
| **STACK** | `STACK.md` (in repo) | Every pinned dependency → its version → its official docs URL. |
| **OPEN-WORK** | `OPEN-WORK.md` (in repo) | What is owed, wanted, settled and still open. Split out of this file 2026-08-21 — it is the half that moves every slice. **Imported by this file**, so unlike the rows above it arrives on its own (LAW §3). |
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

**The same staleness fails the other way, which is worse, and it does not need rsync.** Change a method's signature in `src/main` and `./mvnw test-compile` can answer `BUILD SUCCESS` while every call site in `src/test` still passes the old arity — the incremental compiler does not always notice that a changed main class invalidates test classes. A false *pass* is not a wasted hour like the false failure above; it is a green build you report as green. Widening `Experience.create` by one parameter did exactly this: fifteen breakages, reported as success, until `rm -rf target/test-classes target/classes` forced the issue. **After changing a signature anything else calls, clear the classes before believing a compile.**

## Where this stands

Clean rebuild. **Thirteen contexts** are built — identity · notification · reference · touroperator · media · experience · audience · pickup · audit · page · metafield · contact · storefront — on the `shared` kernel. Trunk is `main`; every PR merges with the full suite green, and the `docker compose up` runtime gate is the user's to run.

**Built:** the admin API in full, the operator's own surface (brand, store policies, structured address, SEO, translations, locales, menus, custom data), and the storefront's data contract. **Not built:** the transaction half — cart → checkout → bookings — and everything that makes a storefront look like one, since `storefront` still answers JSON rather than HTML. Themes, sections and the theme model are all open; **the pages themselves are not** — every storefront address serves its real document as of 2026-08-22.

The storefront was built, deleted whole on 2026-08-02, rebuilt in-process, and cut back to a placeholder on 2026-08-11 — the last of those stand-in routes went on 2026-08-22, so **nothing in the storefront is a placeholder any more**; what is unbuilt is the rendering, not the data. **Treat those deletions as history, not as the current shape** — the live contract and render path are `PATTERNS.md` §2a and §2b. The deleted separate-renderer repo is a git bundle outside the tree (`~/storefront-archive.bundle`, 20 commits); it had no remote.

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
| `touroperator` | The tenant aggregate, its team, and everything an operator configures about itself. **Create** (`POST /api/tour-operators`, creator→OWNER, operator + owner in one tx; **single owner per operator** and **one default operator per user** are DB-enforced via partial unique indexes; welcome email). **Read/edit** `GET`+`PATCH /api/tour-operators/{id}` over name, address, phone, email, timezone and currency — `handle` is deliberately absent, it is the storefront subdomain. **The address is structured** (`address1`, `address2`, `city`, `province`, `zip`, `country_id` → `reference.country`) and **PATCH replaces it whole**: merging `{"city": "Barcelona"}` into a Madrid address would produce a plausible-looking wrong address rather than a partial one. **Team:** invitations (invite/list/get/resend/revoke, ADMIN+-write member-read; public preview/accept with verified-from-birth provisioning) + members (roster, role change, ownership transfer, remove/leave). **The membership view carries resolved reference values, not ids** — the IANA timezone name and the ISO 4217 currency *code*, not the symbol, because `Intl.NumberFormat` derives symbol and per-locale decimals from the code. Sub-resources, each one-per-operator and so addressed without an id: **`/brand`** (slogan, short description, four media ids, and two ordered child tables — the palette is `colors.primary[0].background`, so **position comes from the payload order and is an address a theme indexes**; social platforms are a closed list retargeted away from Shopify's nine to the ones an operator actually runs). **`/seo`**, **`/locales`**, **`/translations/{locale}`** (composite PK, every column nullable so it overlays; `name`/`handle`/`address` deliberately untranslated — a brand name is not content). **`/policies/{type}`** over a closed four-value `PolicyType` — **here and not in `page`** because the defining trait is one per operator from a fixed set. **`/storefront-password`** — `password_enabled` + a **plaintext** password by design (a shared gate the operator reads back and hands out, Shopify's model, never a credential), and **the password value never reaches the audit trail**. **Menus** — `menus` (immutable handle, renameable title) + a self-referencing `menu_items` tree, position-ordered, 3-level cap, link types HOME/EXPERIENCE_LIST/EXPERIENCE/PAGE/EXTERNAL_URL validated against the ownership ports; `PUT .../menus/{id}/items` **replaces the whole tree wholesale** (fresh ids, one audit event). Every operator gets two menus at creation, handles `main-menu` and `footer` (titles "Main menu" and "Footer") — and **the handle is what `linklists` is keyed on**, so a theme reads `linklists["footer"]`, never the title. Both enum pairs are pinned to their CHECK constraints **both ways** — enum-not-in-CHECK fails the write with an untranslated 23514, CHECK-not-in-enum fails a read on a public page. Still no status or fee fields. | Built + merged |
| `audience` | Pax pricing tiers (Adult, Child, …) — own context, operator-scoped, reused across slots. CRUD minus delete under `/api/tour-operators/{id}/audiences` (member-read/admin-write; name unique per operator **case-insensitive**, 409). Update is PARTIAL and propagates name/paxPerUnit onto slot snapshots via `SlotAudienceSnapshotPropagator` (#47). Per-locale **name translations** (#52, 4 endpoints mirroring experience translations). **DELETE removed (#53)** pending a product decision on deletion-vs-snapshot semantics. Ships `AudienceOwnershipQuery` for experience. | Built + merged (#45/#47/#52/#53) |
| `experience` (slots) | + **Slots + per-audience pricing** (#46/#48): a slot = a bookable departure, operator-LOCAL wall-clock `startAt`/`endAt` (`LocalDateTime`, duration derived, ≤24h, cross-midnight self-describing), snapshots experience name/description (kept in sync on experience edit, #48); timing immutable — cancel + recreate. Status AVAILABLE/SOLD_OUT/CANCELLED, and **none of the three is an operator toggle**: SOLD_OUT is derived from bookings at checkout (nothing writes it yet), CANCELLED has its own endpoint, and the slot PATCH edits capacity only. `audience_slot` rows: price+capacity **frozen at create**, name/paxPerUnit synced, bare `audience_id` (no FK — outlives deletion), `booked_count` (0 until bookings exist; DB CHECK ≤ capacity). 6 endpoints: single + recurring create, list (soonest-first), get, cancel, PATCH status/capacity. Also **raw media refs exposed on reads** (#44: `thumbnailMediaId` + `mediaIds`). | Built + merged |
| `pickup` | Pickup locations **catalog only** (name unique per operator case-insensitive + local meeting time). 5 CRUD endpoints under `.../pickup-locations` (partial PATCH; `context:"pickup-locations"`). **A standalone catalog is the finished state, not a stepping stone** — slots know nothing about pickups and nothing is waiting to wire them up. The relationship shipped in #49 (synced snapshots) and was **removed entirely in #50**; `experience` V6 drops the never-populated V5 table. | Built + merged (catalog); relationship unwired by decision |
| `audit` | Platform-wide **append-only audit trail** (#54). Every operator-facing mutation (29 use cases wholesale) appends via `shared/port/AuditTrailPort` **inside the business tx** — "no unaudited mutation" (entry rolls back with the action); S3-backed mutations append after the write instead. `actor_name` frozen at write (filter-only, not sortable); `request_id` via the correlation filter; actors USER/SYSTEM (SYSTEM has no emitter yet, by decision). Read: `GET .../audit-log` (cursor list) + `/{entryId}`, member-visible. | Built + merged |
| `reference` (languages) | + `reference.languages` master list (`GET /api/languages`, seeded en/es/fr/it/pt/de) — the content-language allowlist operators validate against. | Built + merged |
| `experience` | The operator's sellable product. CRUD + publish/unpublish under `/api/tour-operators/{id}/experiences`; member-read/admin-write; media validated-on-write (`MediaAssetBatchQuery`) and resolved-on-read; handle per-operator unique and **immutable**; `published` and `featured` booleans. **Per-locale translations**: nullable overlay fields + an optional localized handle, validated against `OperatorLocalesQuery`. **Canonical and localized handles are one namespace** (PATTERNS §4d) — the derived canonical handle suffixes past both, an explicit localized handle 409s against both. **`starting_price`** (`NUMERIC(12,2) NOT NULL DEFAULT 0`) is the storefront's "from" figure, operator-set since #119; the admin does not send it yet. **SEO overrides** on the experience and its overlay, authored through the existing create/update and translation-upsert paths — no use case of their own. It implements `StorefrontExperienceQuery`: `findFeatured` for the globals' cards and `findPublishedHandles` for menu links, both in the rendered locale. **`findByHandle` landed with the detail page** (2026-08-21) — localized handle first, canonical second, and the canonical 404s in a locale that renames it. **That makes a §4d shadowing handle observable for the first time**: the write guards have existed since 2026-08-01 with nothing consulting both namespaces in precedence order, so a canonical handle another experience publishes as a localized one now resolves to the wrong experience rather than merely being storable. Checked against the dev data when it shipped: zero shadows. Slots and pricing are the row below; a pickup link and delete are still out. **Categories** (V14/V15) are a module in this context, not a context of their own — a category only classifies experiences, so it earns no boundary (LAW §2.2) and `experiences.category_id` is a plain intra-schema FK rather than a shared port. Operator-owned and CRUD'd, so **not Shopify's `product.category`** (a global taxonomy nobody edits) — closer to their free-text `product.type` with a stable id behind it. Flat, no tree: name only, unique per operator case-insensitively (409), **no handle** because nothing routes to a category yet. 5 CRUD endpoints + 4 translations, audited, entity type `CATEGORY`. **`category_id` is nullable and uncategorized is a state, not a gap** — `ON DELETE SET NULL`, so deleting a category leaves its experiences behind rather than refusing or taking them. The reference is validated **422** at the experience write boundary (`CategoryReferenceValidator`), not 404, because on a PATCH the experience does exist and it is the body that is wrong. The experience PATCH is a whole replace, so **an omitted `categoryId` clears it** — the trap the SEO pair fell into before #145. Nothing on the storefront reads a category yet. | Built + merged |
| `media` | Operator media library. Upload (multipart, ADMIN+) / list (cursor, member) / get (member) / **describe** (`PATCH .../media/{id}`, ADMIN+) / delete (ADMIN+) under `/api/tour-operators/{id}/media`. Member-read / admin-write; storage_key only (URLs resolved at read); allowlist images+PDF ≤25MB (SVG excluded by decision — script injection); ships the **`MediaAssetBatchQuery`** cross-context seam (renamed from `MediaKeyBatchQuery` in #100 when it began carrying alt and dimensions rather than a bare key), consumed by `UpdateBrandUseCase`, experience's `MediaReferenceValidator` and `shared.media.MediaUrlBatchResolver`. **Dimensions are measured at upload (#109)** through `ImageDimensionsPort` — a port because `javax.imageio` is not `java.*` — reading the header only and answering empty for a PDF, an unknown format or a damaged header, so it can never fail an upload. **`alt` is written afterwards**, since only the uploader knows it. Rows predating #109 keep null dimensions; nothing backfills. | Built + merged |
| `page` | **CMS content pages** (#56) — merchant-authored About/Contact/FAQ/policy content, served at `/pages/{handle}`. Title + an **operator-chosen handle** (unique per operator; collision = **409, never auto-suffixed** — it is the permanent URL), raw-HTML body (≤256 KiB, stored verbatim, NUL rejected — escaping is a render concern), SEO overrides (Shopify's admin limits), a `published` **boolean** (**no scheduling**, by decision) — it was a DRAFT/PUBLISHED status enum until #142 swapped it for experiences' shape, on the wire as well as in the column, because the storefront asks both the same question and a menu item can point at either. **Rename is its own endpoint** — changing a permanent URL is a deliberate act. Per-locale translations mirror experience's (nullable overlay + localized-handle rules: explicit → 409, derived-with-probing, absent → canonical serves the locale). 12 endpoints (8 + 4 translation); audited from day one. Ships `PageOwnershipQuery` **and `StorefrontPageQuery`** (`findPublishedHandles` for menu links, `findByHandle` for the page itself, both in the rendered locale), implemented in `infrastructure/query`. That seam was deleted 2026-08-02 with the serving side and **rebuilt**; `/pages/{handle}` and `/{locale}/pages/{handle}` are live routes on `StorefrontCmsPageController`, not a plan. | Built + merged |
| `metafield` | **Custom data — the operator's own schema**, both halves in one context (metaobjects did not earn their own). **Metafields**: namespace.key definitions per (operator, owner type) over an 8-type catalogue using **our own type codes**, not Shopify's `*_field` forms; ownerType/namespace/key/type are **immutable** and delete cascades values. Values are **owner-generic** — `MetafieldOwnerType {EXPERIENCE, PAGE, TOUR_OPERATOR}`, reaching owners through `ExperienceOwnershipQuery`/`PageOwnershipQuery`. `TOUR_OPERATOR` needs no seam at all: the owner *is* the tenant, so `ensureOwned` is an equality, and its mount carries **no owner id in the path**. **The price of owner-generic is a bare `owner_id` with no FK**, so the database cannot cascade: **any owner that can be deleted must call `MetafieldValueCleanup` inside its delete transaction** (`page` does; experiences have no delete yet). **Metaobjects**: custom content types — definitions with ordered, renameable fields (the last one is not removable) + entries with an operator-chosen handle, row-per-field values and a `published` boolean. **`metaobject_reference` wires them together**: a reference-typed definition pins one metaobject type, values validate type+ownership, deleting a referenced entry **clears the pointing values in the same tx**, and a pinned type cannot be deleted while targeted (409). 45 endpoints, audited day-one. **The line to hold: a metafield is content the storefront renders and nothing else understands.** Anything with behaviour — hours that decide bookability — needs real columns and domain rules; "just make it a metafield" is how a schema quietly stops being able to answer questions. | Built + merged |
| `contact` | **Contact-form inbox — the admin half only, by decision** (#63). `contact_messages`: UUIDv7 id so reverse-chron *is* `-id`, bare `tour_operator_id`, **required name** (nullable by mistake until contact/V3 — an inbox a person reads and replies to is worse off with an address and no name), email, summary, verbatim TEXT content. The row is immutable: read-state was dropped in #130 and `ContactMessageRepository.save` went with it. 3 endpoints — cursor list, get, delete (ADMIN+, audited with the **summary only**, so the sender's email stays out of the trail). **Intake is deleted and the inbox is a mailbox nothing can fill.** `SubmitContactMessageUseCase` was the only creator; `ContactMessage.submit` and the four validation value objects are **parked deliberately rather than deleted**, because the intake returns and the domain is what makes that cheap. What it established stays true for then: throttled per tenant 30/h **before** validation, so rejected input costs a token too, and **not audited by decision** — the row is its own record, so a `STOREFRONT` audit actor is still unneeded. | Built + merged |
| ~~`rendering`~~ | **Deleted 2026-08-02** — the whole serving side, when the storefront ran as a separate renderer calling in over HTTP. Every decision it embodied was a consequence of that shape, which is why none of it was ported forward. Recover from git if a piece turns out to fit. | Deleted |
| `storefront` | The public site, multi-tenant, resolved from the request host. **Serves JSON, not HTML** — `/` and `/{locale}` serve the globals contract (`tourOperator`, `localization`, `routes`, `linklists`, `featuredExperiences`, page SEO, `canonicalUrl`, `pageType`), `/pages/{handle}` adds `page`, both `/experiences` forms add `experiences: {data, nextCursor}`, both `/experiences/{handle}` forms add `experience`, and both `/policies/{type}` forms add `policy` — **every address serves its own document now, none answers a stand-in**. **The language switcher offers each page in each language** — per-locale handles cross the ports as `LocalizedHandles`, and every route's `canonicalUrl` is read back off the current language's own entry so the two cannot drift. **The listing takes a cursor and nothing else** — no filters, no sorts, `published = true` added by the adapter, so a visitor cannot reach a predicate rather than being refused one. It deliberately does **not** use `ListQueryParser`: that answers 422 to any unknown parameter, which would make every `?utm_source=` link an error page. The password gate is live and a new store is private by default. **The full record — the contract, the render path and what is still open — is `backend/PATTERNS.md` §2a and §2b.** It is kept there and not here: the storefront is backend design, and it was previously written out in three places. | Built (data contract); no templates yet |
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

`storefront` resolves the tenant from the host and owns **eleven addresses**. Ten are pages, and **all ten now serve real data** — five routes, each with and without a locale prefix: `/`, `/experiences`, `/experiences/{handle}`, `/policies/{type}`, `/pages/{handle}`. The eleventh is **`/password`**, the gate — `GET` renders it, `POST` submits it, and it is a route like any other, which is why `PasswordPageController` is one of the six controllers below.

There are **six controllers**: `StorefrontHomeController`, `StorefrontExperienceListController`, `StorefrontExperienceDetailController`, `StorefrontCmsPageController`, `StorefrontPolicyController`, `PasswordPageController`. **Each page route names its own `pageType` and canonical path** at the `StorefrontGlobalsResponse` factory it calls — `index`, `list-experiences`, `experience`, `page`, `policy` — rather than having them inferred from which objects are present, because a route whose object is absent would otherwise become the index by accident. The experiences listing is the live instance of that hazard: its body is byte-identical to the home page's apart from those two fields. **Every factory is named; there is no public `from`** (#203), and `SwitcherUrlsRoundTripThroughLocaleRuleTest` reflects over the class so a new one that mints switcher urls fails the build until it is covered — it caught the policy factory. The tenant seam is `TenantHandleResolver` plus `StorefrontTourOperatorQuery`. There is no `StorefrontTenantQuery` — it was never rebuilt under that name. The password gate is back in full.

**A policy's public slug is not its enum name.** `PolicyType.LEGAL_NOTICE` is addressed as `/policies/legal-notice`, and `storefront.application.policy.PolicySlug` owns the transform both ways — the globals mint the url, the route answers at it, and one class means they cannot disagree. It **cannot validate**, since `storefront` may not import `PolicyType`; an unknown slug becomes a name no constant has and the read answers empty. So an unwritten policy and a slug that is not a policy type are the same 404, deliberately.

An unknown handle and an unpublished locale both answer 404 in the application's usual error shape. They are deliberately indistinguishable.

**It answers JSON, not HTML, on purpose.** The contract-first phase is for getting the *data* right before themes exist, and markup nobody reads hides a wrong field. Mustache stays for the same reason: the dependency and `StorefrontMustacheConfig`'s compiler are the render-path decision waiting for themes, and its settings test keeps the traps in `STACK.md` true meanwhile.

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

`servesHeadAsWellAsGet` pins it, in five tests covering **nine of the eleven** addresses — the entry is per route as well as per method, so coverage is per route too. Four of the five test both forms of their route; the CMS test covers only the bare one. **`/{locale}/pages/{handle}` and `/password` have no HEAD test.** Both are registered correctly — `StorefrontPublicRoutes` loops `PAGE_ROUTES` adding GET+HEAD per route, then lists the gate's three methods explicitly — so nothing is broken today. That loop is why registration is not where the risk is any more, and the tests are: a route added to `PAGE_ROUTES` is registered on both methods by construction, while its HEAD test is still written by hand or not at all.

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
- **A commit body is rarer than it looks** (LAW §6.2). The durable *why* belongs in the doc that owns the rule — `PATTERNS.md` for a recipe, `OPEN-WORK.md` for a decision; the reviewer's context belongs in the PR description; the diff belongs in git. A message that repeats all three is paying three times.

## Open work

Debt, backlog, settled decisions and the open ones live in `OPEN-WORK.md`, which is
**imported below, so it is in context from the first message of every session** —
the same mechanism the root uses for LAW. LAW §3 requires that: a repo may split
its state across files, and every one of them must be auto-loaded, because
auto-loading is the property that matters and not the filename.

It left this file because it is the half that changes every slice while the rest
describes a shape that mostly holds. **Splitting it without the import is the
failure mode** — the load-bearing half goes unread while every pointer to it still
reads correctly, which is what #204 review caught before this line existed.

**The split buys structure, and costs context.** Claude Code's docs are explicit
that imported files load at launch alongside the file importing them, so
*"splitting into `@path` imports helps organization but doesn't reduce context"*.
Measured at the split: **547 lines before, 627 after** — **+80**, spent on a
header, the pointer section and the paragraphs explaining the arrangement.

A `backend/` session auto-loads **four files**: LAW, the root `CLAUDE.md`, this,
and `OPEN-WORK.md`. **The total is deliberately not written down here** — it moves
every slice, so a number in this paragraph is stale before it is read, and a stale
number is exactly what LAW §0.3's enforcement mechanism cannot use. Run
`wc -l CLAUDE.md OPEN-WORK.md ../CONSTITUTION.md ../CLAUDE.md` when the question
is live; `/context` says what actually loaded.

What was bought is that this file reads as the shape on its own, which is real and
was the point. What was not bought is room. Do not split anything else expecting
to save any — the only way to spend less is to write less, which is what LAW §0.3
means by subtracting from the system first and the docs second.

**Where you launch decides how this loads.** A `CLAUDE.md` at or above the working
directory loads in full at launch; one in a *subdirectory* loads on demand when a
file there is read, and is not re-injected after `/compact`. So this file and its
import arrive at startup when you run from `backend/`, and lazily when you run from
the repo root. `/context` lists what actually loaded — that is the check, not
inference from the file being on disk.

@OPEN-WORK.md

## Working rhythm

Trunk is `main`; no direct commits. Every slice gets a short-lived branch (`feat/…`, `fix/…`, `chore/…`, `docs/…`) → PR → **merge only when the user says so**. A slice is done when the full suite is green *and* the change has been verified live against the running stack.

End any session that changed something with the landing ritual (LAW §3): make this file, `OPEN-WORK.md`, `PATTERNS.md`, `STACK.md` and any saved memory true. A version trap goes in `STACK.md`, a pattern that has now repeated twice in `PATTERNS.md`, a decision or a piece of owed work in `OPEN-WORK.md`, and the per-context row in *What each context owns*. If a fact moved, delete the old copy rather than leave it to drift.

**This file and `OPEN-WORK.md` are the only reason the next session knows where things stand.** They took that job from `MAP.md`, deleted 2026-08-20 — *what is built* here, *what is owed or decided* there — so a claim in either is load-bearing in a way the rest of the prose is not, and goes stale the same way MAP's did. Grep the claim, not the file you happen to be reading.

**A move falsifies more than the pointers into it.** This paragraph said "this file is the only reason" and named *owed or decided* as its own, and both halves stopped being true the moment the ledger left — while the sweep for stale references correctly found nothing, because a claim about what a file is *complete for* leaves no link behind to grep. So when something moves out of a doc, re-read what the doc says about itself, not only what points at it.

**And re-read what the docs above it say about it.** LAW §3 named this file as the home for decisions and gave auto-loading as the reason, so the split falsified LAW too — in a different git repo, where no grep of this one reaches. That took a LAW amendment (v0.6.1) and the import above, not a wording fix here. The rule that generalises: a doc's scope is asserted in three places — in itself, in what points at it, and in whatever governs it — and only the middle one is greppable from where you are standing.
