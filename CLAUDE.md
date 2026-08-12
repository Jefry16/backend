# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Read these first

Three of the four governing documents are **not linked from anywhere in this repo** — load them yourself:

| Document | Location | What it is |
|---|---|---|
| **LAW** | `../CONSTITUTION.md` (the parent directory — its own git repo, tracking LAW and MAP only) | The rules. Short, read whole, every session. |
| **MAP** | `../MAP.md` (same place) | The living state: what exists, what each context owns, what is decided, what is still open. The only artifact that crosses session boundaries. |
| **PATTERNS** | `PATTERNS.md` (in repo) | The recipes. Before building anything, find the matching one — don't reverse-engineer existing code. |
| **STACK** | `STACK.md` (in repo) | Every pinned dependency → its version → its official docs URL. |
| **CONTEXT-AUDIT** | `CONTEXT-AUDIT.md` (in repo) | The playbook for auditing one bounded context — dead code, over-engineering, coupling. Invoke with just a context name. |

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

**ArchUnit enforces the boundaries** (`src/test/java/com/vointika/architecture/ArchitectureTest.java`), so a violation is a failing test, not a review comment. Context isolation is **derived from the package structure** — a new context is fenced the day its package appears, with no rule to remember to add. (It used to be one hand-written rule per context; seven contexts landed without one, and the rules that existed only named the original four.)

### The rules that shape every change

- **A context never imports another context.** Two channels only: a **shared query port** (`shared.port.<Noun>Query` + a `<Noun>View` of primitives, implemented by the owning context in its `infrastructure/query`) or a **Kafka event**. `shared` and `reference` are the shared kernels everyone may import.
- **Use cases are plain POJOs** — no Spring annotations — hand-wired as `@Bean`s in each context's `infrastructure/config/<Ctx>UseCaseConfig`. **ArchUnit enforces an allowlist**: the application layer may depend on `com.vointika..` and `java..` and **nothing else** — no third-party library at all, not even a logging facade. Reaching for one is the signal that a port is missing. **`java..` does not cover `javax..`**: `javax.crypto` fails the rule exactly like a third-party jar, which is why the storefront's unlock-cookie HMAC is `UnlockTokenPort` + an adapter rather than a policy class (PATTERNS §8d). Best-effort side effects (a storage delete, a broker publish) log in the adapter that fails; a use case with something of its own to report uses `DiagnosticLogPort`.
- **Every operator-facing mutation appends to the audit trail inside the same transaction** as the mutation (PATTERNS §8b). No unaudited mutation.
- **Any list over tenant or growable data uses the shared list framework** — keyset cursor, typed filters, `ListSchema` (PATTERNS §4b). Never return an unbounded array; that mistake has already been made and fixed once.
- **URLs are never stored.** Store a storage key, resolve to a URL at read time.
- **Responses identify themselves with `id` + `context`** — never `userId`/`tourOperatorId`, never `type` (PATTERNS §4a).

### One API surface, one auth model

**The admin/operator API** — JWT bearer tokens. There is no `/api/storefront/**` surface and **no shared secret anywhere**: the storefront does not call in over HTTP, it renders in-process. Tenant-scoped routes live under `/api/tour-operators/{tourOperatorId}/**` and are gated in **two layers**: a membership interceptor (non-member → **404**, byte-identical to a missing operator) plus per-use-case role gates (`ensureAdmin`/`ensureOwner`) through the `TourOperatorMembershipCheck` port. Authorization belongs in the use case, not only the router — the router's matching is looser than the id binder, which is how an IDOR got in once.

**The storefront serves its globals as JSON, and its auth decision is still "none yet."** It was cut back to a placeholder on 2026-08-11 (`e379cf2`) and the home page came back the same day. `storefront` resolves the tenant from the host and owns six addresses — `/`, `/{locale}`, `/experiences`, `/{locale}/experiences`, `/policies/{type}`, `/{locale}/policies/{type}`. **`/` and `/{locale}` serve the real contract**: `shop` (brand, palette, social links, policies, currency, timezone), `localization`, `routes` and `pageTitle`/`pageDescription`/`ogImageUrl` — the same set Shopify's index template gets, since that template has no object of its own. The other four still answer `{"handle","status"}`. An unknown handle, and a locale the shop does not publish, are both a 404 in the application's usual error shape — deliberately indistinguishable. **JSON and not HTML until the contract is settled**: a wrong field is visible in a body and invisible under markup nobody reads yet. **Everything that read a storefront's data is deleted**: the shop/brand/policy/experience queries (`StorefrontShopQuery`, `StorefrontExperienceQuery`), the theme object model, all six page use cases, every view and every template, and the password gate. What is left is `TenantHandleResolver` + `StorefrontTenantQuery.exists(handle)` + one controller. **It answers JSON, not HTML, on purpose**: the point of the placeholder period is getting the *data* right before themes exist, and markup nobody reads hides a wrong field. Mustache itself stays — the dependency and `StorefrontMustacheConfig`'s compiler are the render-path decision (MAP open decision 6) waiting for the themes, and its settings test is what keeps the traps in `STACK.md` true meanwhile. The design record survives in PATTERNS §2a (marked as a specification, not a description) and in git; rebuild from those rather than from memory. Routes stay unauthenticated through the same `PublicRouteRegistrar` every other public route uses, not a host-matched `SecurityFilterChain` — nothing needs storefront requests to have *different* security, only *no* authentication.

**The password gate is back, and a new store is private by default.** `CreateTourOperatorUseCase` generates a storefront password and sets `password_enabled` at creation, so an operator's store exists at its address and answers the gate rather than the shop — Shopify's model, where a new store is password-protected until the merchant is ready. Existing operators were left open. The operator reads the password back through `GET /api/tour-operators/{id}/storefront-password` and disables the gate when it wants to sell.

**The rule that cost the most to learn is pinned again: the gate runs before locale resolution.** Resolve the locale first and a locked store 404s an unpublished locale while redirecting a published one, telling an anonymous visitor the store exists and which locales it has. `StorefrontHomeControllerTest.aLockedStoreRedirectsEvenForALocaleItDoesNotPublish` is the guard, and it is why the gate landed *after* the index slice — it needs `LocaleRule` to order against. A gated store still answers on its address with the gate page (Shopify leaks existence the same way); it is not a 404. The unlock cookie is `HMAC-SHA256(key = storefront_password, message = operatorId)`, so rotating the password invalidates every outstanding cookie for free.

Public (unauthenticated) routes are opt-in per context via the `PublicRouteRegistrar` SPI; rate-limit rules likewise via `RateLimitRuleRegistrar`. Never add either to a central hardcoded map.

**A `PublicRoute` pattern is a security pattern before it is a route, so an unconstrained path variable is a hole.** `/{locale}` reads like one page route and `permitAll`s **every single-segment path in the application** — `/error` today, whatever `/health` or `/metrics` lands later, silently. Constrain the variable and define the pattern **once** for the mapping, the `PublicRoute` entries and any interceptor patterns (`StorefrontRoutes.LOCALE`). The group must be non-capturing — `PathPatternParser` rejects capture groups outright (PATTERNS §11).

**A `PublicRoute` matches one HTTP method, so a page route needs `GET` *and* `HEAD`.** Spring MVC serves HEAD from a `@GetMapping` for free; Spring Security does not — a GET-only entry rejects HEAD at the filter chain as a **401 in the JSON error shape**, never reaching MVC. Harmless on a JSON API nobody HEADs, wrong on a public page: crawlers, link checkers, uptime monitors and CDNs all send HEAD. `storefront` shipped without it because every test and curl used GET; it took a request against the built stack to find. Pinned by `servesHeadAsWellAsGet` in `StorefrontPlaceholderControllerTest`, which walks every address, because the entry is per route as well as per method. Counting the `@GetMapping`, **a page route is registered in three places** — four while the password gate exists, since its interceptor needs every page pattern too; define its pattern once in `application/policy` (PATTERNS §11).

### Migrations

Flyway runs **once per domain**, each into its own Postgres schema, in the order listed in `shared/infrastructure/flyway/FlywayPerDomainConfig.DOMAINS` — the order matters wherever cross-schema FKs exist. A new context adds its folder `src/main/resources/db/migration/<ctx>/` **and** an entry in `DOMAINS`. **Never modify an applied migration**; add the next `V`. Curated reference/seed data lives in the migration itself; dev-only fixtures live in `docker/dev-seed/`.

Renames must sweep beyond `src/` — the dev seed and docs reference table and column names too.

### Tests

Unit tests (JUnit 5 + Mockito, no Spring) for value objects, entity behavior, and use cases. Controller tests are **RestDocs documentation tests** — `@WebMvcTest` + `@Import(SecurityConfig.class)`, asserting behavior *and* emitting the snippets that build the API guide. Three recurring traps: a `@WebMvcTest` whose context loads `WebConfig` needs a `@MockitoBean TourOperatorMembershipCheck` or every request 500s; **any storefront `@WebMvcTest` needs a `@MockitoBean CheckStorefrontLockUseCase`** for the same reason, because `StorefrontWebConfig` is a `WebMvcConfigurer` and every slice therefore registers the gate's interceptor, which resolves that use case per request; and a public-route test that omits its `PublicRouteRegistrar` 401s everything, so the assertions pass without testing anything.

## Conventions

The working rules are LAW: §2.4 never over-engineer · §3 the landing ritual · §4 never assume · §6 craft (comments, commits, dead code). Only the calibration for this repo lives here.

- **Javadoc runs heavier than LAW §6.1's default, deliberately.** A hexagonal context has real seams, and the Javadoc on a port, a security filter or a migration is often the only place a decision is recorded — `EndpointRateLimitFilter`'s note on why the counter keys on the matched *pattern* and not the concrete URI is load-bearing. The rule still bites: `/** Returns the user. */` over `getUser()` is noise. Keep the why, the trap and the rejected alternative; delete the restatement.
- **Dead code has no mechanical gate here.** Java offers no `noUnusedLocals` equivalent, so LAW §6.3 is a look — plus **ArchUnit**, which already fences the §2 boundaries and is the right home for anything automatable (a port nobody implements, a use case no `@Bean` wires).
- **A commit body is rarer than it looks** (LAW §6.2). The durable *why* belongs in `MAP.md`; the reviewer's context belongs in the PR description; the diff belongs in git. A message that repeats all three is paying three times.

## Working rhythm

Trunk is `main`; no direct commits. Every slice gets a short-lived branch (`feat/…`, `fix/…`, `chore/…`, `docs/…`) → PR → **merge only when the user says so**. A slice is done when the full suite is green *and* the change has been verified live against the running stack.

End any session that changed something with the landing ritual (LAW §3): make `../MAP.md`, this file, `PATTERNS.md`, `STACK.md` and any saved memory true. A version trap goes in `STACK.md`, a pattern that has now repeated twice in `PATTERNS.md`, and if a fact moved, the old copy is deleted rather than left to drift. MAP is the only reason the next session knows where things stand.
