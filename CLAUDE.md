# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read these first

**Nothing loads these for you.** Four javadoc comments name `PATTERNS.md` and `STACK.md`, so the code knows they exist — but no import pulls one in and no build step reads one. Load them yourself:

| Document | Location | What it is |
|---|---|---|
| **LAW** | `../CONSTITUTION.md` (the parent directory — its own git repo, tracking LAW and MAP only) | The rules. Short, read whole, every session. |
| **MAP** | `../MAP.md` (same place) | The living state: what exists, what each context owns, what is decided, what is still open. The only artifact that crosses session boundaries. |
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

## Architecture

A modular monolith: `com.vointika.<context>`, one package per bounded context, each **fully hexagonal** with the layer DAG `domain ← application ← {infrastructure, presentation}`. `domain` is pure — no Spring, JPA, or Jackson. A context that owns no entities has no `domain` (`notification`, `storefront` — PATTERNS §1); the DAG is unchanged, and in those contexts it bites harder, because `presentation` and `infrastructure` still may not reach each other.

**ArchUnit enforces the boundaries** (`src/test/java/com/vointika/architecture/ArchitectureTest.java`), so a violation is a failing test, not a review comment. Context isolation is **derived from the package structure**, so a new context is fenced the day its package appears. There is no rule to remember to add. It used to be one hand-written rule per context. Seven contexts then landed without one, and the rules that did exist only named the original four.

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

`servesHeadAsWellAsGet` in `StorefrontPlaceholderControllerTest` pins it, and walks every address, because the entry is per route as well as per method.

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
- **A commit body is rarer than it looks** (LAW §6.2). The durable *why* belongs in `MAP.md`; the reviewer's context belongs in the PR description; the diff belongs in git. A message that repeats all three is paying three times.

## Working rhythm

Trunk is `main`; no direct commits. Every slice gets a short-lived branch (`feat/…`, `fix/…`, `chore/…`, `docs/…`) → PR → **merge only when the user says so**. A slice is done when the full suite is green *and* the change has been verified live against the running stack.

End any session that changed something with the landing ritual (LAW §3): make `../MAP.md`, this file, `PATTERNS.md`, `STACK.md` and any saved memory true. A version trap goes in `STACK.md`, a pattern that has now repeated twice in `PATTERNS.md`, and if a fact moved, the old copy is deleted rather than left to drift. MAP is the only reason the next session knows where things stand.
