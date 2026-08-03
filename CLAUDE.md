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

**The storefront is the second surface, and its auth decision is "none yet."** `storefront` server-renders the public site in-process (MAP open decision 6) and registers its four routes — `/`, `/{locale}`, `GET`+`POST /password` — through the same `PublicRouteRegistrar` every other public route uses, not a host-matched `SecurityFilterChain`. That is where it ends up, but nothing today needs storefront requests to have *different* security, only *no* authentication: `JwtAuthenticationFilter` ignores anonymous requests and `ApiRateLimitFilter` passes through anything without a JWT principal. Per-IP limits still go through `RateLimitRuleRegistrar` like everyone else's (`POST /password`, 20/h). Revisit when the storefront needs its own CORS or error shape.

**What gates the storefront is a `HandlerInterceptor`, not the security chain.** The password gate is a per-tenant, per-request decision on data, so it sits in `storefront/infrastructure/web` on the storefront's own patterns — `/` and `/*`, **never `/**`**, which would front `/api/**`. It runs **before** locale resolution, and that ordering is the load-bearing part: gate second and a locked store 404s an unpublished locale while redirecting a published one, which tells an anonymous visitor the store exists and which locales it has. `StorefrontHomeControllerTest.aLockedStoreRedirectsEvenForALocaleItDoesNotPublish` is that ordering; nothing else pins it.

Public (unauthenticated) routes are opt-in per context via the `PublicRouteRegistrar` SPI; rate-limit rules likewise via `RateLimitRuleRegistrar`. Never add either to a central hardcoded map.

**A `PublicRoute` matches one HTTP method, so a page route needs `GET` *and* `HEAD`.** Spring MVC serves HEAD from a `@GetMapping` for free; Spring Security does not — a GET-only entry rejects HEAD at the filter chain as a **401 in the JSON error shape**, never reaching MVC. Harmless on a JSON API nobody HEADs, wrong on a public page: crawlers, link checkers, uptime monitors and CDNs all send HEAD. `storefront` shipped without it because every test and curl used GET; it took a request against the built stack to find. Pinned by `servesHeadAsWellAsGet` in both storefront controller tests — one per page route, because the entry is per route as well as per method.

### Migrations

Flyway runs **once per domain**, each into its own Postgres schema, in the order listed in `shared/infrastructure/flyway/FlywayPerDomainConfig.DOMAINS` — the order matters wherever cross-schema FKs exist. A new context adds its folder `src/main/resources/db/migration/<ctx>/` **and** an entry in `DOMAINS`. **Never modify an applied migration**; add the next `V`. Curated reference/seed data lives in the migration itself; dev-only fixtures live in `docker/dev-seed/`.

Renames must sweep beyond `src/` — the dev seed and docs reference table and column names too.

### Tests

Unit tests (JUnit 5 + Mockito, no Spring) for value objects, entity behavior, and use cases. Controller tests are **RestDocs documentation tests** — `@WebMvcTest` + `@Import(SecurityConfig.class)`, asserting behavior *and* emitting the snippets that build the API guide. Two recurring traps: a `@WebMvcTest` whose context loads `WebConfig` needs a `@MockitoBean TourOperatorMembershipCheck` or every request 500s; and a public-route test that omits its `PublicRouteRegistrar` 401s everything, so the assertions pass without testing anything.

## Conventions

The working rules are LAW: §2.4 never over-engineer · §3 the landing ritual · §4 never assume · §6 craft (comments, commits, dead code). Only the calibration for this repo lives here.

- **Javadoc runs heavier than LAW §6.1's default, deliberately.** A hexagonal context has real seams, and the Javadoc on a port, a security filter or a migration is often the only place a decision is recorded — `EndpointRateLimitFilter`'s note on why the counter keys on the matched *pattern* and not the concrete URI is load-bearing. The rule still bites: `/** Returns the user. */` over `getUser()` is noise. Keep the why, the trap and the rejected alternative; delete the restatement.
- **Dead code has no mechanical gate here.** Java offers no `noUnusedLocals` equivalent, so LAW §6.3 is a look — plus **ArchUnit**, which already fences the §2 boundaries and is the right home for anything automatable (a port nobody implements, a use case no `@Bean` wires).
- **A commit body is rarer than it looks** (LAW §6.2). The durable *why* belongs in `MAP.md`; the reviewer's context belongs in the PR description; the diff belongs in git. A message that repeats all three is paying three times.

## Working rhythm

Trunk is `main`; no direct commits. Every slice gets a short-lived branch (`feat/…`, `fix/…`, `chore/…`, `docs/…`) → PR → **merge only when the user says so**. A slice is done when the full suite is green *and* the change has been verified live against the running stack.

End any session that changed something with the landing ritual (LAW §3): make `../MAP.md`, this file, `PATTERNS.md`, `STACK.md` and any saved memory true. A version trap goes in `STACK.md`, a pattern that has now repeated twice in `PATTERNS.md`, and if a fact moved, the old copy is deleted rather than left to drift. MAP is the only reason the next session knows where things stand.
