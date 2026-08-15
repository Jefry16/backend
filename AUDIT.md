# Codebase audit — `vointika/backend` — 2026-08-15 (second pass)

Investigation only. No code changed.

This replaces the first 2026-08-15 pass. That one is in git (`2f7ef75`) — the first
report to survive its own session, because the tracked-report rule landed with it.

**What changed since the first pass:** the prompt gained four contradiction sweeps
(`REPO-AUDIT.md`), and they are the reason for this rerun. They found three things
the first pass missed, and the first pass missed them because it found
contradictions by accident rather than by looking.

Tags: **NEW** · **KNOWN-OPEN** (recorded, still true) · **REGRESSION** (was fixed,
is back) · **STALE-RECORD** (recorded, but the record is now wrong).

---

## Baseline

```
rsync -a --delete --exclude target/ --exclude .git/ ./ <scratch>/be/
rm -rf <scratch>/be/target/classes <scratch>/be/target/test-classes
./mvnw -o clean test -Dtest='!VointikaApplicationTests'
```

**1200 tests, 0 failures, 0 errors.**

`VointikaApplicationTests.contextLoads` is excluded because it needs a live Postgres
and I cannot run Docker. With it, the suite is 1201 across 213 classes — and 213
matches the number of test files on disk, so nothing is being silently skipped.

What that exclusion costs: **Flyway's per-domain migration order and
`ddl-auto: validate` are unverified in this pass.** To close it:

```
docker compose run --rm app ./mvnw -o test -Dtest=VointikaApplicationTests
```

---

## 1. The password gate is live, and `PATTERNS.md` says three times that it is gone

- **Files**: `PATTERNS.md:892-893`, `PATTERNS.md:924`, `PATTERNS.md:693`
- **Severity**: **high** · **Tag**: **NEW**

The storefront password gate came back in #138. `PATTERNS.md` still describes it as
deleted, in three separate sections:

**§11, the route-registration recipe** (`:891-894`):

> It was **four** places while the password gate existed — its interceptor needed
> every page pattern too, or a locked store served the page — and it goes back to
> four when the gate does.

Past tense, and "when the gate does" means the gate is not here. It is:

```
storefront/infrastructure/web/StorefrontLockInterceptor.java
storefront/infrastructure/web/StorefrontWebConfig.java   ← 2 addInterceptor calls
```

**§11, the `WebMvcConfigurer` gotcha** (`:924`):

> `StorefrontWebConfig` was the second example until the password gate it
> registered was deleted.

It is the second example right now.

**§8d, the `javax.*` lesson** (`:693`):

> (Both went with the placeholder cutback; the lesson is why this paragraph stays.)

Both are here, exactly where the sentence above says they landed:

```
storefront/application/port/UnlockTokenPort.java
storefront/infrastructure/security/HmacUnlockToken.java
```

**Why this one is high.** §11 is the recipe a session reads *before* adding a
storefront route. It tells them a route needs two or three registrations and that
the gate's interceptor is not among them. That is wrong, and it is wrong in the
direction that ships a page a locked store serves to anyone.

It also compounds. Finding 4 below says the guard test cannot catch that mistake.
So a developer adding a route gets told by the recipe that they need not gate it,
and then gets a green build confirming it.

**One root cause, not three findings.** #138 rebuilt the gate and never swept
`PATTERNS.md`. This is the same failure the previous audit named — renames and
removals not being swept — pointed the other way: a *restoration* not being swept.

- **Fix**: three sentences. Put §11's count in the present tense (four places, three
  registries — the wording MAP settled in #157), make `StorefrontWebConfig` a current
  example, and delete §8d's parenthetical. 20 minutes.
- **Verified by**: the three lines quoted from `PATTERNS.md`; `find src/main/java` for
  all four class names, each present; `grep -c "addInterceptor" StorefrontWebConfig.java`
  → 2. Found by grepping the docs for absence claims (`was deleted|is gone|no longer|
  until .* deleted`), which is sweep 3 of the new prompt.

---

## 2. `PATTERNS.md` and `CLAUDE.md` disagree about how many places a route is registered

- **Files**: `PATTERNS.md:891-894` vs `CLAUDE.md:84` vs `../MAP.md` (#157 entry)
- **Severity**: medium · **Tag**: **NEW**

Same fact, three files, two answers.

`CLAUDE.md:84` is right:

> Counting the `@GetMapping`, **a page route is registered in three places** — four
> while the password gate exists

MAP's #157 entry settled the wording: *"four registrations across three registries"*.

`PATTERNS.md` says it was four "while the password gate existed".

A reader who checks the recipe file — which is what LAW §5.2 tells them to do —
gets the wrong number. A reader who checks `CLAUDE.md` gets the right one. Nothing
says which to trust.

- **Fix**: covered by finding 1's edit. Listed separately because it is a different
  failure — not a stale sentence, but two docs that cannot both be true, with no
  mechanism that would ever notice.
- **Verified by**: all three passages read and quoted; the code supports `CLAUDE.md`
  and MAP.

---

## 3. `PATTERNS.md` §4e's heading says six; its own body says eight, nine lines down

- **File**: `PATTERNS.md:458` vs `:467`
- **Severity**: medium · **Tag**: **KNOWN-OPEN** (first pass §5.1 — still unfixed)

```
458:## 4e. The translation-overlay table (six of them, in two shapes)
467:**Eight tables do this**, in **two shapes**. Six are *column-shaped* — nullable
```

PR #157 fixed the table and the conclusion — the previous audit's top finding — and
left the heading. The body is right: 8 translation tables, 4 `overlay()` copies.

- **Fix**: one word. **Verified by**: `grep -n "six of them\|Eight tables do this" PATTERNS.md`;
  `grep -rhoE "CREATE TABLE [a-z_.]*translations" src/main/resources/db/migration/ | sort`
  → 8; `grep -rln "private static String overlay(" src/main/java` → 4.

---

## 4. A storefront route written as a literal is invisible to the lock interceptor

- **Files**: `storefront/infrastructure/web/StorefrontWebConfig.java:45-46`;
  guard at `src/test/java/com/vointika/storefront/infrastructure/security/StorefrontRouteRegistriesTest.java:53-77`
- **Severity**: **high** · **Tag**: **KNOWN-OPEN** (first pass §3.1)

The gate is registered on exactly `StorefrontRoutes.PAGE_ROUTES`. The guard test
reflects over that class's *constants*; it never reads a controller's `@GetMapping`.
A route written as a literal string is outside everything it checks.

Proven earlier today by mutation against byte-identical code: adding
`@GetMapping("/sitemap.xml")` plus the two `PublicRoute` entries a developer must add
for it to answer at all gives **1200 tests, 0 failures, BUILD SUCCESS**. The route is
public and ungated. A locked store serves it to anyone.

Not re-mutated in this pass — the tree is unchanged (`diff -rq` clean against the
scratch copy) and re-running would produce the same output.

- **Fix**: check the registries against the mappings, not the constants. Walk
  `RequestMappingHandlerMapping`, or parse the four controllers' `@GetMapping` values,
  and require every storefront pattern to be in `PAGE_ROUTES` or the existing
  `NOT_A_PAGE_ROUTE` exception set. 2–3 hours, test-only.
- **Read finding 1 with this one.** They are the same hazard from two directions: the
  recipe says you need not gate the route, and the build agrees.

---

## 5. PR #163 is flagged in MAP but still has no ledger entry

- **File**: `../MAP.md:18`
- **Severity**: medium · **Tag**: **KNOWN-OPEN**, partially addressed since the first pass

MAP's header now says #163 merged without an entry. That stops the next session
being unaware of it. It does not record what #163 did, and the five open findings in
its PR description are still nowhere else. Four are confirmed in the code:

| Claim | Confirmed at |
|---|---|
| `sectionFor`'s `start < 0 ? 0` silently widens a section to the whole guide prefix | `ApiGuideDocumentsEveryListFieldTest.java:258` |
| the missing-clause assertion throws inside `forEach`, so only the first break reports | `:121` |
| `SCHEMA_BLOCK` needs the literal `SCHEMA = ListSchema.builder()`, so an inline schema is unchecked | `:69` |
| `describe()`'s `new LinkedHashMap<>(wrong)` is a dead copy of a `TreeMap` | `:266` |

The fifth — sections list filter fields but not the operators — is about guide prose
and I did not re-derive it.

- **Fix**: one build-ledger entry, five items into Debt. 20 minutes, and it belongs to
  whoever ran #163.
- **Verified by**: `grep -n "#163" ../MAP.md` → one hit, in the header, not the ledger;
  the four line numbers grepped.

---

## 6. Three small doc-drift items

All **NEW** unless marked, all low, all a few minutes each.

**`docker-compose.yml:115`** documents a diagnostic deleted four days ago:

```yaml
      # Dev only: `?format=json` returns a page's theme context instead of the page.
      APP_JWT_ACCESS_TOKEN_EXPIRATION_MS: 900000
```

`ThemeContextDump` and `app.storefront.context-endpoint` both went in the 2026-08-11
cutback. The env var the comment described went with them. The comment now sits above
an unrelated line and reads as documentation of the JWT expiry. **Verified by**: two
greps over `src/`, both empty.

**`STACK.md:19-27`** lists five pinned images; `docker-compose.yml` pins six. The
`ses` service runs `node:22.23.1-alpine` with `aws-ses-v2-local@2.10.0`, and it is the
service `CLAUDE.md` tells a session to read sent mail from. "Dev-only tooling is
omitted" does not cover it — the MinIO client is equally dev-only and has a row.
**Verified by**: `grep -nE "image:" docker-compose.yml` against the table.

**`../MAP.md:3077-3078`** (**STALE-RECORD**) says the unbounded metafields read is at
`.../{experiences|pages}/{id}/metafields`. There are three now, not two —
`TourOperatorMetafieldController.java:53` has the same shape since #139. The recorded
decision is unaffected; the scope is understated. **Verified by**:
`grep -rn "ResponseEntity<List<" --include=*Controller.java` → 17, of which 3 return
`MetafieldValueResponse`.

**`PATTERNS.md:703`** files §4c between §8d and §9. A reader looking for it after §4b
will not find it. (§8c is absent for a good reason — it was deleted with the storefront
serving side in `7dbbc86`, and renumbering would break every reference. That one is not
a finding.) **Verified by**: `grep -n "^## " PATTERNS.md`;
`git log -S"## 8c" -- PATTERNS.md`.

---

## 7. Dead code — none

Every scan came back empty. That is the result, not a skipped check.

| Scan | Result |
|---|---|
| `TODO` / `FIXME` / `HACK` / `XXX` | 0 |
| Commented-out code | 0 |
| Unused imports (831 files) | 0 |
| Use cases with no `@Bean` (147 checked) | 0 |
| Ports with no implementation | 0 |
| `app.*` config keys nothing reads (23) | 0 |
| `System.out` / `printStackTrace` | 0 |

**Looks dead, is not**: 66 classes are referenced by name nowhere else. All 66 are
Spring beans wired by type — `*RepositoryImpl`, `*UseCaseConfig`, `*QueryImpl`, the
five Kafka consumers, `CorsConfig`, `UuidV7IdGenerator`, `VointikaApplication`.

**Verified by**: per-symbol import search over the whole main tree; a shell loop
testing each `*UseCase.java` basename against every `*Config.java`; a class-name
search across `src/main`, `src/test`, `src/main/resources`, `src/docs` and `docker/`.

---

## 8. Invariants — one gap, the rest hold

Each was mutation-checked: break it, watch the build.

| Invariant | Result |
|---|---|
| A context never imports another | **Fires.** Names file and line. |
| Application layer touches only `com.vointika..` + `java..` | **Fires.** Names field and call site. |
| Every page route is gated | **Does not fire** — finding 4. |
| Seed matches migrations (§10) | 0 problems across 34 inserts, 42 tables |
| Audit append inside the transaction (§8b) | 0 of 72 outside |
| `id` + `context` response identity (§4a) | holds |
| Lists use the shared framework (§4b) | 15 schemas ↔ 15 cursor controllers |

**§10 is the one with no build gate**, and it has broken three times. I modelled all
42 tables from the migrations — `CREATE TABLE` plus every `ALTER … ADD/DROP/RENAME
COLUMN` and `SET/DROP NOT NULL`, handling unqualified table names, quoted identifiers
and digit-bearing column names — and checked all 34 `INSERT`s in `dev-seed.sql`. No
column the schema lacks. No required column missing from an INSERT.

Three earlier hits were bugs in my own parser (`address1` needed
`[a-z_][a-z0-9_]*`, `"time"` needed quote handling, `duration_minutes` needed
unqualified-`ALTER` support). Fixed, not reported.

**§8b**: 12 of 147 use cases do not audit. All 12 are `identity` — the user's own
account — plus `storefront`'s unlock. None is operator-facing, which is what the rule
actually requires.

---

## 9. Bad practices — one known, nothing new

**`GlobalExceptionHandler.java:83-86`** (**KNOWN-OPEN**) still reports any
`IllegalArgumentException` as the client's fault with the internal message attached,
four lines above a catch-all that deliberately hides messages. No reachable path
today. It is a standing pre-commitment, not a live defect.

Checked and clean:

- **CORS** — explicit origin (`http://localhost:3000`), never `*`, with
  `allowCredentials(true)`. No environment override widens it.
- **JWT secret** — `application.yml:98` has no default, so the app cannot boot on a
  fallback key.
- **N+1** — `AudiencePricingResolver.validateAndResolve` runs once at
  `CreateSlotsUseCase:96-97`, outside the date loop. Audience lookups do not multiply
  by slot count.
- **Mutable shared state** — none.
- **Empty catches** — none; all rethrow or log.

---

## 10. Comment noise — 4 banners, nothing else

18% density (6,373 of 34,603 lines), which is the deliberate calibration in
`CLAUDE.md`: heavy Javadoc on ports, filters, migrations and seams.

| | |
|---|---|
| Commented-out code | 0 |
| Authorship / changelog | 0 |
| Generation narration | 0 |
| Unactionable TODOs | 0 |
| Section banners | 4, all in `GlobalExceptionHandler` |
| Restatement candidates | 2, both keepers |

Only `GlobalExceptionHandler.java:26` (`// --- Domain exceptions ---`) is clearly
informationless — the annotation below it already says that. The other three carry
ordering or a why. This is close to a style preference and I am labelling it as one.

**Keepers, by name**, so a future sweep does not take them: `EndpointRateLimitFilter`
on keying by matched pattern; `TourOperatorMembershipInterceptor:24-27` on the IDOR
that a tighter regex once allowed; `JwtAuthenticationFilter:58-61`;
`ArchitectureTest:41-54,155-170`; `StorefrontRoutes:89-98,101-118`;
`SortableColumnsAreNeverNullableTest:21-59`; `FlywayPerDomainConfig:28-31`;
`V15__structured_address.sql:1-18`; and `Audience.java:26/31` +
`PickupLocation.java:26/32`, which look like restatement and are not — each
disambiguates one of two constructors.

---

## UNVERIFIED

- **Flyway ordering and `ddl-auto: validate`.** `contextLoads` could not run. Command
  above.
- **Finding 4 against a real locked store.** I proved the suite does not catch it and
  read the registration that explains why. I did not watch a gated store serve an
  ungated route over HTTP.
- **The fifth #163 finding** — filter operators in the guide prose. Not re-derived.
- **`ReplaceMenuItemsUseCase` at scale.** No item cap, verified by reading. Actual
  query count needs a running Postgres.

---

## Fix these five, in order

1. **Finding 1 — the three stale gate sentences in `PATTERNS.md`.** Twenty minutes,
   and it is first because it actively misleads. §11 is the file a session opens
   before adding a storefront route, and it currently tells them not to gate it.

2. **Finding 4 — make the route guard read mappings, not constants.** The only
   finding with a security consequence, proven rather than argued. Together with
   finding 1 it closes both halves of the same hazard.

3. **Finding 3 — the §4e heading.** One word. It is here because that section was
   the *previous* audit's top finding, was fixed, and the fix left the headline
   stating the number it existed to correct.

4. **Finding 5 — land #163 in MAP.** Twenty minutes. Five identified defects
   currently live only in a PR description, which no session reads.

5. **Finding 6 — the three one-line doc fixes.** Ten minutes for all three. The
   `docker-compose.yml` one matters slightly more than its size: it does not just
   describe something gone, it now appears to describe the line beneath it.

---

*Audit reports are tracked (policy set 2026-08-15, reversing PR #160). A report is a
dated snapshot; `../MAP.md` is where anything durable lives. Superseding a report
replaces this file — git holds the history. The playbook that produced it is
`REPO-AUDIT.md`.*
