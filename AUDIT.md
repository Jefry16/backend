# Codebase audit — `vointika/backend` — 2026-08-15

Investigation only; no code was changed. Every mutation used to prove a guard fires was
applied to a scratch copy outside the repo and reverted (`diff -rq` clean, shown below).

> **This supersedes the 2026-08-14 pass, which has been deleted.** That report's findings
> were closed by PRs #157–#162; what remained open is carried below as KNOWN-OPEN rather
> than re-argued. It is **not recoverable** — `AUDIT.md` is untracked and the file was
> `git rm --cached`ed in `e4f38b0`, so no copy survives in git or on disk. The same is true
> of `API-DOCS-AUDIT.md` (the 2026-08-14 API↔REST Docs sync audit, closed by #159), deleted
> alongside it.
>
> Neither loss is durable: `../MAP.md` carries the lasting half of both by design — its
> #156 build-ledger entry states outright that the API-docs report was a local artifact and
> that MAP is where its durable half lives.

Every finding carries a tag:

| Tag | Meaning |
|---|---|
| **NEW** | In neither the previous report nor MAP. |
| **KNOWN-OPEN** | Already recorded and still true. Pointer only. |
| **REGRESSION** | Recorded as fixed, and is back. |
| **STALE-RECORD** | Recorded as open, but the record is now wrong. |

---

## Baseline — what was run

```
rsync -a --delete --exclude target/ --exclude .git/ ./ <scratch>/be/
rm -rf <scratch>/be/target/classes <scratch>/be/target/test-classes
./mvnw -o clean test
```

**1201 tests across 213 test classes. 1200 pass; 1 errors.**

The count is sound: 213 classes reported by Surefire **equals** 213 `*Test.java`/`*Tests.java`
files in `src/test/java`. (The `clean` matters — the 2026-08-14 pass published 1298/225 by
aggregating a stale `target/surefire-reports/` that still held reports from deleted test
classes.)

**The one error is environmental, not a defect.** `VointikaApplicationTests.contextLoads` is
`@SpringBootTest` and needs a live Postgres:

```
Caused by: org.postgresql.util.PSQLException: Connection to localhost:5432 refused.
```

I cannot run Docker (session constraint), so this test could not execute. Every
delete-and-rerun check below therefore ran as `-Dtest='!VointikaApplicationTests'` against a
**1200**-test baseline. See UNVERIFIED for what this leaves unchecked.

**What was read.** LAW, MAP (all sections), `PATTERNS.md`, `STACK.md`, `CLAUDE.md`,
`CONTEXT-AUDIT.md`, `pom.xml`, `application.yml`, `docker-compose.yml`, `docker/dev-seed/`;
`SecurityConfig`, `JwtAuthenticationFilter`, `GlobalExceptionHandler`, `CorsConfig`,
`CorsProperties`, `DocsPublicRoutes`, `TourOperatorMembershipInterceptor`,
`StorefrontWebConfig`, `StorefrontRoutes`, `StorefrontPublicRoutes`, all four storefront
controllers, `AudiencePricingResolver`, `CreateSlotsUseCase`, `ArchitectureTest`,
`StorefrontRouteRegistriesTest`, `SortableColumnsAreNeverNullableTest`,
`ApiGuideDocumentsEveryListFieldTest`.

---

## 1. DEAD CODE

**None found.** Every mechanical scan came back empty, and the emptiness is the result, not a
skipped check:

| Scan | Result |
|---|---|
| `TODO` / `FIXME` / `HACK` / `XXX` across `src/` | **0** |
| Commented-out code (`// if(`, `// return`, `// public`, …) | **0** (3 regex hits were all continuation lines of prose comments) |
| Unused imports in `src/main/java` (831 files, per-symbol body search) | **0** |
| Use cases with no `@Bean` in any `*Config.java` (147 checked) | **0** |
| Ports / query seams with no `implements` | **0** |
| `app.*` keys in `application.yml` with no `@Value` or properties accessor (23 keys) | **0** |
| `System.out` / `System.err` / `printStackTrace` | **0** |
| `@Deprecated` | **0** |

**Verified by**: `grep -rn "TODO\|FIXME\|HACK\|XXX" src/`; a Python pass that, for each
`import x.y.Z;`, searches the file body (imports stripped) for `\bZ\b`; a shell loop testing
each `*UseCase.java` basename against `src/main/java/**/*Config.java`; a Python pass mapping
each `app.*` leaf key to its camelCase accessor and grepping `src/main/java`.

### Uncertain — looks dead, is not

**66 classes are referenced by name nowhere else in the repository**, and all 66 are
Spring-managed beans wired by *type*: `*RepositoryImpl` (`@Repository`), `*UseCaseConfig`
(`@Configuration`), `*QueryImpl` (`@Component`), the five notification `@KafkaListener`
consumers, `CorsConfig`, `MediaUrlConfig`, `KafkaConfig`, `DocsPublicRoutes`,
`UuidV7IdGenerator`, `VointikaApplication`.

`UuidV7IdGenerator` was the only one worth tracing individually, because it is a `@Component`
in `shared/service` with no `Impl` suffix to explain it: it `implements IdGenerator`, and
`IdGenerator` is a constructor parameter at 6 sites in `TourOperatorUseCaseConfig` alone.

**Verified by**: a Python pass building the set of all 831 main-tree class names and searching
every file under `src/main`, `src/test`, `src/main/resources`, `src/docs` and `docker/` for
each; then `cat src/main/java/com/vointika/shared/service/UuidV7IdGenerator.java` and
`grep -rn "IdGenerator" src/main/java`.

---

## 2. OVER-ENGINEERING

**None new.**

- **KNOWN-OPEN** — `contact_depends_only_on_shared` (`ArchitectureTest.java:85`) has nothing
  to catch since the storefront intake was deleted on 2026-08-01. Recorded in the previous
  report §2.2, and the rule carries a comment at `:78-83` saying exactly this and why it is
  kept. Not re-argued.
- **Not over-engineering, checked**: the seven single-adapter ports. The application layer's
  ArchUnit allowlist admits only `com.vointika..` and `java..`, so a port is frequently the
  *only* legal way to reach a library — `JsonSyntaxPort`, `UnlockTokenPort`,
  `ImageDimensionsPort` each exist for that reason, not for a second implementation.

---

## 3. UNDER-ENGINEERING

### 3.1 A storefront page route added as a literal is invisible to the lock interceptor, and the whole suite stays green

- **Files**: `storefront/infrastructure/web/StorefrontWebConfig.java:45-46`;
  `storefront/application/policy/StorefrontRoutes.java:119-123`;
  guard at `src/test/java/com/vointika/storefront/infrastructure/security/StorefrontRouteRegistriesTest.java:53-77`
- **Severity**: **high**
- **Tag**: **NEW**

`StorefrontWebConfig` registers the gate on exactly `StorefrontRoutes.PAGE_ROUTES`:

```java
registry.addInterceptor(new StorefrontLockInterceptor(tenantHandleResolver, checkStorefrontLock))
        .addPathPatterns(StorefrontRoutes.PAGE_ROUTES.toArray(String[]::new));
```

Deriving the registries from one list closed the drift **between** registries. What is left is
the assumption that every page route becomes a constant on `StorefrontRoutes` — and nothing
enforces it. `StorefrontRouteRegistriesTest` reflects over
`StorefrontRoutes.class.getDeclaredFields()`; it never reads a controller's `@GetMapping`
value. A route written as a literal is outside everything it checks.

This is the exact failure the guard's own Javadoc says it exists to prevent — *"Three of the
four agreeing is a state nothing detected. The page answers, so every test and every curl
passes, while a store the operator locked serves that page to anyone."*

**Proven, not reasoned.** In the scratch copy I added to `StorefrontPlaceholderController`:

```java
@GetMapping(path = "/sitemap.xml", produces = MediaType.APPLICATION_JSON_VALUE)
public StorefrontPlaceholderResponse sitemap(HttpServletRequest request) {
    return new StorefrontPlaceholderResponse("sitemap");
}
```

plus the two `PublicRoute` entries a developer must add for it to answer at all (without them
Spring Security 401s it, which fails *closed* and is noticed immediately — so the realistic
mistake is adding them):

```java
routes.add(new PublicRoute(HttpMethod.GET,  "/sitemap.xml"));
routes.add(new PublicRoute(HttpMethod.HEAD, "/sitemap.xml"));
```

Result: **`Tests run: 1200, Failures: 0, Errors: 0` — BUILD SUCCESS.** The route is public and
ungated; a locked store serves it to an anonymous visitor, and nothing in the build says so.

- **Fix**: assert the registries against the *mappings*, not the constants — walk
  `RequestMappingHandlerMapping` (or parse the four controllers' `@GetMapping` values) and
  require every storefront pattern to be in `PAGE_ROUTES` or the declared exception set. The
  existing `NOT_A_PAGE_ROUTE` set already gives the escape hatch. Cost: 2–3 hours, one test
  file, no production change.
- **Verified by**: both mutations applied to the scratch copy and run
  (`./mvnw -o test -Dtest='!VointikaApplicationTests'` → 1200/0/0, BUILD SUCCESS); the
  interceptor's single `addPathPatterns(PAGE_ROUTES)` call read at `StorefrontWebConfig:45-46`;
  `StorefrontRouteRegistriesTest:56` confirmed to iterate `getDeclaredFields()` and nothing
  else; scratch copy restored and `diff -rq --exclude=target --exclude=.git` against the repo
  reports no differences.

### 3.2 Two unbounded fan-out loops on admin write paths

- **Severity**: low · **Tag**: **KNOWN-OPEN** (previous report §3.5; MAP build ledger, #162 entry)
- Still true, unchanged. `ReplaceMenuItemsUseCase` issues one ownership query per distinct
  EXPERIENCE/PAGE link with no item-count cap; `CreateSlotsUseCase` can mint ~730 slots in one
  transaction.
- Re-checked one thing the record does not state, because it would change the severity:
  `AudiencePricingResolver.validateAndResolve` is called **once** at
  `CreateSlotsUseCase.java:96-97`, *outside* the date loop, and the loop uses `buildRows`
  against the already-resolved list. So the audience lookups do **not** multiply by slot count.
- **Verified by**: `CreateSlotsUseCase.java:85-120` and `AudiencePricingResolver.java:41-58`
  read end to end; a heuristic scan for repository/port calls within 14 lines of a loop
  construct across `src/main/java` produced 64 candidates, all inspected — the rest are calls
  whose *result* is streamed, not calls per iteration.

### 3.3 Three security filters have no test of their production registration

- **Severity**: low · **Tag**: **KNOWN-OPEN** (previous report §3.4)
- Unchanged, and it is now the second finding this report attributes to the same root cause as
  §3.1: what is registered in production is asserted by construction in a unit test rather than
  observed in the assembled chain.

---

## 4. BAD PRACTICES

### 4.1 `@ExceptionHandler(IllegalArgumentException.class)` reports internal faults as the client's fault

- **File**: `shared/web/exception/GlobalExceptionHandler.java:83-86`
- **Severity**: low · **Tag**: **KNOWN-OPEN** (previous report §4.1)
- Still present, still with no reachable path, and still four lines above a catch-all at
  `:90-94` that deliberately does the opposite (`log.error(...)` then a fixed message). It
  remains a standing pre-commitment rather than a live defect.
- **Verified by**: file read in full; the handler is unchanged from the previous pass's quote.

**Nothing else found in this category.** Specifically checked and clean:

- **CORS** — `CorsConfig.java:19-31` sets `allowCredentials(true)` with
  `allowedOrigins(properties.allowedOrigins())`, and `application.yml:77` resolves to
  `${APP_CORS_ALLOWED_ORIGINS:http://localhost:3000}`. An explicit origin, never `*`; the
  wildcard-with-credentials combination Spring rejects at runtime cannot arise from the default.
  `allowedHeaders("*")` is safe given explicit origins. **Verified by**: both files read;
  `grep -n "CORS" docker-compose.yml` returns nothing, so no environment override widens it.
- **JWT secret** — `application.yml:98` is `secret: ${APP_JWT_SECRET}` with **no default**, so
  the application cannot boot on a fallback key. The literal in `docker-compose.yml` is a
  dev-only value that names itself as one. **Verified by**:
  `grep -nE "secret|password|key" src/main/resources/application.yml`.
- **Audit-inside-transaction (§8b)** — **0 of 72** auditing use cases call `auditTrail.append`
  outside a `transactionRunner.run(...)` block. Of the 147 use cases, 12 do not audit at all,
  and all 12 are `identity` (the user's own account: login, logout, register, verify, reset,
  change password, change language, set avatar, refresh, resend) plus `storefront`'s
  `UnlockStorefrontUseCase` — none operator-facing, which is what §8b actually requires.
  **Verified by**: a Python pass computing the paren-balanced span of every
  `transactionRunner.run(` and testing each `auditTrail.append` offset against those spans;
  plus a name-based sweep for mutating use cases lacking any `AuditTrailPort` reference, and
  each of the 12 resolved to its context by file path.
- **Mutable shared state** — no non-final static field in `src/main/java`.
- **Dead-letter of errors** — no `System.out`/`printStackTrace`; the empty-looking catch blocks
  all rethrow or log (checked by printing 2 lines of context after every `catch (`).
- **`@SuppressWarnings`** — 7 occurrences, all `unchecked`/`rawtypes` on the Criteria list
  executor, `ValueCoercion`, `ListQueryParser` and the two deliberate Boot-4 Kafka raw-type
  injections. No `@SuppressWarnings("all")` and nothing hiding a nullability or deprecation
  warning.
- **`/docs/**` is public** — `DocsPublicRoutes.java:14-21` `permitAll`s `GET /docs/**`. It is a
  deliberate decision stated in the class Javadoc (the generated REST Docs guide, endpoint
  shapes only) and the pattern is scoped to that prefix.

---

## 5. DRIFT

### 5.1 `PATTERNS.md` §4e's heading says six tables; its own body says eight, nine lines later

- **File**: `PATTERNS.md:458` vs `PATTERNS.md:467`
- **Severity**: medium · **Tag**: **NEW** (introduced by the fix in #157)

```
458:## 4e. The translation-overlay table (six of them, in two shapes)
467:**Eight tables do this**, in **two shapes**. Six are *column-shaped* — nullable
```

PR #157 corrected the table and the conclusion drawn from it — the previous audit's
highest-severity finding — and left the heading. The heading is the line a reader scanning the
file for a recipe sees first, and it now states the number the fix existed to correct. The body
is right: there are exactly **8** translation tables and exactly **4** `overlay()` copies.

- **Fix**: change "six of them" to "eight of them". Cost: one word. While in there, the phrase
  "two shapes" is used at `:467` for column-shaped vs row-shaped and again at `:502` for
  multi-column vs single-column — two different axes, same words, 35 lines apart.
- **Verified by**: `grep -n "six of them\|Eight tables do this\|Two shapes, and the split" PATTERNS.md`;
  `grep -rhoE "CREATE TABLE [a-z_.]*translations" src/main/resources/db/migration/ | sort` →
  exactly 8 (`audience`, `experience`, `metafield_value`, `metaobject_entry_value`, `page`,
  `menu_item`, `tour_operator_policy`, `tour_operator`);
  `grep -rln "private static String overlay(" src/main/java` → exactly 4.

### 5.2 MAP has no entry for PR #163, and five of its open findings exist only in a PR description

- **Files**: `../MAP.md` (build ledger); PR #163, merged as `5d01502`
- **Severity**: medium · **Tag**: **NEW**

`grep -n "#163" ../MAP.md` returns nothing. The PR merged on 2026-08-15 and changed
`api-guide.adoc` and `ApiGuideDocumentsEveryListFieldTest` (+85/−11). Its description records
five deliberately-deferred findings on that test, and **all five are verifiable in the code
today**:

| Claim in #163 | Confirmed at |
|---|---|
| `sectionFor`'s `start < 0 ? 0` silently widens a section to the whole guide prefix | `ApiGuideDocumentsEveryListFieldTest.java:258` |
| the missing-clause assertion throws inside `forEach`, so only the first broken section reports | `:121` |
| `SCHEMA_BLOCK` requires the literal `SCHEMA = ListSchema.builder()`, so an inline schema is unchecked | `:69` |
| `describe()`'s `new LinkedHashMap<>(wrong)` is a dead copy of a `TreeMap` | `:266` |
| sections list filter fields but not the operators the overview promises | (guide prose; not re-verified) |

MAP is the only artifact that crosses session boundaries (LAW §3). A PR body is not read by the
next session, so on the current record this work did not happen and these five findings do not
exist. This is the landing ritual left incomplete, not a code defect.

- **Fix**: one build-ledger entry in `../MAP.md` naming #163 and carrying the five open items
  into Debt. Cost: 20 minutes. **It is another session's work to land** — flagged, not taken.
- **Verified by**: `grep -n "#163" ../MAP.md` → no output; `gh pr view 163 --json title,body,state,files`;
  `git log --oneline -1 origin/main` → `5d01502 Merge pull request #163`; the four code claims
  grepped at the line numbers above.

### 5.3 `docker-compose.yml` documents a diagnostic that was deleted four days ago

- **File**: `docker-compose.yml:115`
- **Severity**: low · **Tag**: **NEW**

```yaml
      # Dev only: `?format=json` returns a page's theme context instead of the page.
      APP_JWT_ACCESS_TOKEN_EXPIRATION_MS: 900000
```

`ThemeContextDump` and its `app.storefront.context-endpoint` key were both deleted in the
2026-08-11 cutback. The env var the comment described went with them; the comment stayed and is
now attached to an unrelated line, so it reads as documentation of the JWT expiry.

- **Fix**: delete the line. Cost: 1 minute.
- **Verified by**: `grep -rn "ThemeContextDump" src/` → no output;
  `grep -rn "context-endpoint\|contextEndpoint" src/ --include=*.java --include=*.yml` → no
  output; `sed -n '113,118p' docker-compose.yml` for the adjacency.

### 5.4 `STACK.md`'s runtime-services table omits the SES mock

- **Files**: `STACK.md:19-27` vs `docker-compose.yml:93-97`
- **Severity**: low · **Tag**: **NEW**

The table lists five pinned images (Postgres, Redis, Kafka, MinIO, `mc`). `docker-compose.yml`
pins six: the `ses` service runs `node:22.23.1-alpine` with `npx --yes aws-ses-v2-local@2.10.0`.
Both versions are pinned, and this is the service `CLAUDE.md` tells a session to read sent mail
from (`GET localhost:8005/store`). "Dev-only tooling is omitted" does not explain it, since the
MinIO client `mc` is equally dev-only and has a row.

- **Fix**: one table row. Cost: 5 minutes.
- **Verified by**: `grep -nE "image:" docker-compose.yml` (6 distinct pinned images across 8
  services; `seed` reuses `postgres:17.9`, `app` builds locally) against `STACK.md:19-27`.

### 5.5 MAP's unbounded-metafields debt names two owner types; there are three

- **File**: `../MAP.md:3077-3078`
- **Severity**: low · **Tag**: **STALE-RECORD**

The Debt entry reads *"`GET .../{experiences|pages}/{id}/metafields` returns a bare
`List<MetafieldValueResponse>`"*. Since #139 there is a third with the same shape:
`TourOperatorMetafieldController.java:52-53`, mounted at
`/api/tour-operators/{tourOperatorId}/metafields`. The decision recorded in the entry (a
wire-contract choice, not a defect) is unaffected; the scope it states is now understated by a
third.

- **Fix**: widen the entry's wording. Cost: 5 minutes.
- **Verified by**: `grep -rn "ResponseEntity<List<" src/main/java --include=*Controller.java` →
  17 hits; the three `MetafieldValueResponse` ones are `PageMetafieldController:47`,
  `ExperienceMetafieldController:47`, `TourOperatorMetafieldController:53`; MAP lines quoted.

### Checked for drift and found accurate

Each of these was a candidate and each holds:

- `FlywayPerDomainConfig.DOMAINS` lists **11** domains; `db/migration/` holds **11** folders;
  same set. (`notification` and `storefront` own no tables and are correctly absent.)
- Every `STACK.md` pin matches `pom.xml` / `docker-compose.yml`: Boot `4.0.5`, Java `25`, JJWT
  `0.13.0`, AWS SDK `2.42.33`, uuid-creator `6.1.1`, ArchUnit `1.4.1`, REST Docs `4.0.0`,
  asciidoctor-maven `3.1.1`, Postgres `17.9`, Redis `7.4.9`, Kafka `4.3.1`, MinIO/mc releases.
- `CLAUDE.md`'s "eight addresses" and "four controllers" both match: `PAGE_ROUTES` holds exactly
  the eight listed patterns, and `storefront/presentation/controller/` holds exactly
  `StorefrontHomeController`, `StorefrontCmsPageController`, `StorefrontPlaceholderController`,
  `PasswordPageController`.
- **PATTERNS §10, the seed↔migration pairing** — the invariant with no build gate, which has
  broken three times. I modelled all 42 tables from the migrations (`CREATE TABLE` plus every
  `ALTER … ADD/DROP/RENAME COLUMN` and `SET/DROP NOT NULL`, handling unqualified table names,
  quoted identifiers and digit-bearing column names) and checked all **34** `INSERT` statements
  in `dev-seed.sql`: **0 problems** — no column the schema lacks, no NOT NULL-without-default
  column absent from an INSERT. Three earlier "hits" were my parser's bugs
  (`address1`/`address2` needed `[a-z_][a-z0-9_]*`, `"time"` needed quote handling,
  `duration_minutes` needed unqualified-`ALTER` support) and were fixed rather than reported.

---

## 6. COMMENT NOISE

**Density: 18%** — 6,373 comment lines of 34,603 non-blank lines across 831 files. This is
above the previous pass's 15.5%, and it is the recorded deliberate calibration (`CLAUDE.md`:
Javadoc runs heavier than LAW §6.1 on ports, filters, migrations and seams).

| Noise category | Count |
|---|---|
| Commented-out code | **0** |
| Authorship / changelog comments | **0** |
| Code-generation narration (`// Now we handle…`) | **0** |
| Unactionable TODOs | **0** |
| Section banners | **4** |
| Restatement candidates | **2**, both keepers |

### The only flaggable group: 4 banners, all in one file

`shared/web/exception/GlobalExceptionHandler.java` at `:26`, `:75`, `:88`, `:96`:

```java
// --- Domain exceptions ---
// --- Client input errors that Spring MVC surfaces as typed exceptions ---
// --- True unexpected errors (last resort) ---
```

- **Severity**: low · **Tag**: NEW (survivors of #154's 54-banner sweep)
- The first is pure restatement — `@ExceptionHandler(InvalidFieldException.class)` over a
  `com.vointika.shared.exception` type already says "domain exception". The other three carry a
  little more: `"last resort"` states ordering that matters (the catch-all must not shadow), and
  `:96` is a four-line explanation of *why* `ResponseEntityExceptionHandler` is overridden,
  which is a why and would survive on its own without the `---` framing.
- **Fix**: delete `:26`; keep `:88` and `:96` as prose without the banner dashes. Cost: 5
  minutes. This is close to a style preference and I am labelling it as such — only `:26` is
  clearly informationless.

### Keepers, by name

Stated explicitly because a future sweep will meet them and they must not be taken:

- `EndpointRateLimitFilter` — why the counter keys on the matched **pattern**, not the concrete
  URI (a path-variable route would otherwise bucket per token value and never limit).
- `TourOperatorMembershipInterceptor:24-27` — why the gate parses with the *same*
  `UUID.fromString` the `@PathVariable` binder uses; a tighter regex once let lenient UUID forms
  slip the gate (an IDOR).
- `JwtAuthenticationFilter:58-61` — why the principal is the parsed `UUID` and not its text.
- `ArchitectureTest:41-54, 155-170` — why the context fence is derived and why the application
  allowlist is an allowlist rather than a banned-list.
- `StorefrontRoutes:89-98, 101-118` — the `/{locale}`↔`/password` literal collision, and why
  `PAGE_ROUTES` exists rather than three hand-kept copies.
- `SortableColumnsAreNeverNullableTest:21-59` — the silent-row-loss consequence of a nullable
  keyset sort column.
- `FlywayPerDomainConfig:28-31` — the domain order is dependency order, not alphabetical.
- `V15__structured_address.sql:1-18` — why the old free-text column was dropped rather than
  parsed, and what the CHECK constraint is for.
- `Audience.java:26/31` and `PickupLocation.java:26/32` — `/** New audience. */` and
  `/** Reconstitution from persistence. */` look like restatement and are not: each
  disambiguates one of **two** constructors. (Both were my scan's only restatement candidates;
  both resolved to keepers, matching the previous pass's finding.)

**Verified by**: a Python pass over all 831 files counting comment vs code lines and matching
four noise patterns (banner regex, authorship/date regex, narration regex, and a restatement
test requiring every ≥4-char word of a ≤6-word comment to appear in the following code line);
then `grep -n "public Audience(\|/\*\*"` on both entity files to check the overload count.

---

## 7. INVARIANT INTEGRITY

Each guard below was **mutation-checked**: the invariant was broken deliberately in the scratch
copy and the build observed. A guard that passes vacuously is worse than no guard.

| Invariant | Guard | Mutation result |
|---|---|---|
| A context never imports another | `contexts_do_not_depend_on_each_other` | **FIRES**, names file + line |
| Application layer touches only `com.vointika..` + `java..` | `application_depends_only_on_our_code_and_the_jdk` | **FIRES**, names field + call site |
| Every page route is gated | `StorefrontRouteRegistriesTest` | **DOES NOT FIRE** — see §3.1 |
| Seed matches migrations (§10) | *no build gate* | 0 problems (checked externally) |
| Audit append inside the tx (§8b) | *no build gate* | 0 of 72 outside |
| `id` + `context` response identity (§4a) | *no build gate* | holds on all sampled records |
| Lists use the shared framework (§4b) | `SortableColumnsAreNeverNullableTest` (sortability half) | 15 schemas ↔ 15 cursor controllers |

**The context fence fires.** Adding
`static final Class<?> LEAK = com.vointika.touroperator.domain.enums.PolicyType.class;` to
`storefront/application/policy/SeoText.java`:

```
Architecture Violation … Rule 'slices matching 'com.vointika.(*)..' should not depend on each
other' was violated (1 times):
Static Initializer <…storefront.application.policy.SeoText.<clinit>()> references class object
<…touroperator.domain.enums.PolicyType> in (SeoText.java:18)
```

**The application allowlist fires.** Adding an SLF4J logger to the same class:

```
Architecture Violation … should only depend on classes that reside in any package
['com.vointika..', 'java..'] … was violated (2 times):
Field <…SeoText.LOG> has type <org.slf4j.Logger>
```

**§4a holds where it is easiest to get wrong.** `PolicyResponse` and `SlotResponse` both carry
`id` + `context` with the two-constructor pattern; their `type` / `experienceId` /
`audienceId` fields are data and foreign keys, not identity, and `PolicyResponse`'s Javadoc says
so explicitly. Ten `String type` fields across the codebase were checked and every one is a
domain type (`PolicyType`, `MetafieldType`), never a discriminator.

**§4b holds.** 15 `ListSchema.builder()` declarations, 15 controllers using
`CursorPageResponse`. The 17 bare-`List` returns are: 5 reference/ui-language lists (curated,
bounded, exempt by decision), 9 translation/locale lists (bounded by the operator's locale set),
and the 3 metafield value reads of §5.5.

**Verified by**: each mutation applied to `<scratch>/be`, run with
`./mvnw -o test -Dtest=ArchitectureTest`, output quoted above, then reverted;
`diff -rq --exclude=target --exclude=.git . <scratch>/be` reports no differences, so nothing
leaked into the repo.

---

## UNVERIFIED — needs human check

- **Anything requiring the running stack.** I cannot run `docker compose` (session constraint),
  and `VointikaApplicationTests.contextLoads` therefore did not execute — so **Flyway's
  per-domain migration order and `ddl-auto: validate` against the current entities are
  unverified in this pass.** They were green at the last landing. To close it:

  ```
  docker compose run --rm app ./mvnw -o test -Dtest=VointikaApplicationTests
  ```

  (`docker compose build app` alone will not do it — it skips the live-DB path.)
- **The §3.1 leak against a real locked store.** I proved the *test suite* does not catch it and
  read the interceptor registration that explains why. I did not observe an actual gated store
  serving an ungated literal route over HTTP. The code path is short and I am confident, but the
  end-to-end behaviour is inference from two reads plus a green suite, not an observation.
- **`ReplaceMenuItemsUseCase` at scale (§3.2).** Absence of an item cap verified by reading; the
  actual query count and wall-clock for a large tree needs a running Postgres.
- **The fifth #163 finding** — "sections list filter fields but not the operators the overview
  promises". The other four are confirmed at specific lines; this one is a claim about guide
  prose against `ListSchema` filter kinds and I did not re-derive it.

---

## The five things I would fix first

1. **§3.1 — teach the route guard to read mappings, not constants.** It is the only finding here
   with a security consequence, it is proven rather than argued, and it is a gap in a guard
   whose own Javadoc describes precisely this failure. Everything else in this report is prose
   or a known-low. 2–3 hours, test-only.

2. **§5.2 — land #163 in MAP.** Not because a missing ledger entry is dangerous in itself, but
   because MAP is the mechanism the project uses to survive session boundaries, and right now a
   merged PR plus five identified defects are invisible to the next cold session. It is also the
   cheapest item that stops the same five findings being re-discovered by the next audit. 20
   minutes — and it belongs to whoever ran #163.

3. **§5.1 — the `PATTERNS.md` §4e heading.** One word. It is on this list at all because that
   section was the *highest-severity finding of the previous audit*, was fixed, and the fix left
   the headline saying the wrong number — which is the specific way a corrected doc goes on
   misleading people.

4. **§3.3 + §3.1 together — one integration test for the assembled chain.** Both findings say
   the same thing: what production registers is asserted by construction, never observed.
   `@SpringBootTest(webEnvironment = MOCK)` hitting a locked store's page routes and a
   rate-limited endpoint would close both, and would also give `contextLoads` company as the
   only test that sees the whole application. An hour or two, and it retires a category rather
   than a finding.

5. **§5.3 + §5.4 — the two one-line doc fixes.** Bundled because separately neither is worth a
   PR: delete the orphaned `?format=json` comment, add the `ses` row to `STACK.md`. Ten minutes
   for both, and the first is a comment that actively misdescribes the line under it.

---

*Audit reports are **tracked** (policy set 2026-08-15, reversing PR #160, which had untracked
`AUDIT.md` as a point-in-time artifact). A report is a dated snapshot, not a governing doc:
`../MAP.md` remains where anything durable lives, and superseding a report replaces this file
rather than accumulating beside it.*
