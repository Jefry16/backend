Audit this codebase. Do NOT change any code yet — this pass is investigation and
reporting only. Write the report to `AUDIT.md`.

## Scope

**Only `/home/jefrycayo/vointika/backend`.** The frontend repo is out of scope
entirely — do not read it, do not report on it, do not count it in any total. The
governance repo one level up (`../CONSTITUTION.md`, `../MAP.md`) is **input, not
subject**: read it, audit the backend against it, do not audit it.

## Read before judging

In this order, whole, before flagging anything:

- `../CONSTITUTION.md` (LAW) and `../MAP.md` (the living state — long; read
  "State at a glance", "Contexts / slices", "Debt", "Backlog", "Decided",
  "Open decisions" in full and skim the build ledger for the last ~10 entries).
- `CLAUDE.md`, `PATTERNS.md`, `STACK.md`, `CONTEXT-AUDIT.md`.
- `pom.xml`, `application.yml`, `docker-compose.yml`, `docker/dev-seed/`.
- Entry points and shared machinery end to end: `SecurityConfig`, every servlet
  filter, both `HandlerInterceptor`s, `GlobalExceptionHandler`, `shared/list` +
  `shared/infrastructure/list`, `SpringTransactionRunner`, `AuditTrailPortImpl`,
  `RedisRateLimiter`, and every test under `src/test/java/com/vointika/architecture/`.

Build a picture of what the app is supposed to do before flagging anything. It is a
modular monolith: thirteen bounded contexts under `com.vointika.<context>`, each
hexagonal (`domain ← application ← {infrastructure, presentation}`), fenced from
each other by ArchUnit, talking only through shared query ports or Kafka events. Two
surfaces: a JWT-authenticated admin API under `/api/**`, and an in-process
multi-tenant storefront resolved from the request host.

## Prior art — read this before writing a single finding

**`AUDIT.md` is the previous pass, and it is tracked** — read it at whatever date
it carries rather than assuming one. Nine consecutive passes have found dead code,
bad practices and doc drift empty or near-empty. A report that re-lists closed
findings is worse than a short one, because it makes the open items harder to see.

*This paragraph named a fixed date and called the report untracked until
2026-08-15, while the same file's Output section already said reports are tracked.
Exactly the survivor case category 5 below tells you to hunt: a rule changes and
the old wording lives on somewhere nobody swept. Left as a note rather than
silently corrected, because the file asks auditors to report that pattern.*

So, before reporting anything:

1. Read the existing `AUDIT.md` end to end. It is the previous pass.
2. Read MAP's **Debt**, **Backlog**, **Decided** and **Open decisions** sections.
3. Tag every finding with one of:
   - **NEW** — not in either place.
   - **KNOWN-OPEN** — already recorded and still true. One line, a pointer to where
     it is recorded, and whether anything changed. Do not re-argue it.
   - **REGRESSION** — recorded as fixed, and is back. This is the most valuable
     tag in the report; name the PR that closed it.
   - **STALE-RECORD** — recorded as open, but is actually fixed or no longer
     applies. Also valuable: it means MAP is lying to the next session.

Known-open at the time of writing, so expect these and do not present them as
discoveries: the unbounded menu fan-out in `ReplaceMenuItemsUseCase`; the untested
production registration of three security filters; the unreachable
`@ExceptionHandler(IllegalArgumentException.class)`; the four Debt entries in MAP;
and five open review findings on `ApiGuideDocumentsEveryListFieldTest` recorded only
in PR #163's description (filter operators undocumented, `sectionFor`'s
`start < 0 ? 0` fallback, the missing-clause assertion throwing inside `forEach`,
`SCHEMA_BLOCK` silently skipping an inline schema, a dead `LinkedHashMap` copy).

## Do not flag these — they are decisions, not defects

Re-proposing any of these costs a round trip and has already cost several:

- **The storefront password is plaintext.** Forced by the product decision that the
  operator reads it back through `GET .../storefront-password`. Shopify's model.
- **The storefront answers JSON, not HTML.** Deliberate for the placeholder period.
- **Mustache is carried with no live renderer.** It is the render-path decision
  waiting for themes; `StorefrontMustacheConfigTest` keeps `STACK.md`'s traps true.
- **Reference and ui-language lists return plain arrays**, exempt from PATTERNS §4b
  — curated and bounded.
- **`audience_translations` has no storefront reader.** The write half shipped first.
- **The dev database's cleanliness**, and the **absence of `@SpringBootTest`
  integration coverage.** Both accepted and parked.
- **Heavy Javadoc on ports, filters, security classes and migrations.** This is the
  recorded calibration for this repo, not noise — see category 6 below.
- **Four byte-identical `overlay()` copies at 16 call sites** (PATTERNS §4e). The
  arithmetic is recorded; sharing them couples four contexts to remove six lines.
- **Five migration comments still say "shop".** Flyway checksums applied migrations.

If you believe one of these is now wrong, that is a **STALE-RECORD** finding with
evidence — not a fresh proposal.

## Measurement rules — this is where past passes have gone wrong

- **Never run `docker` or `docker compose` yourself.** If a claim needs the live
  stack, write the exact command for the user to run and put the claim in
  UNVERIFIED until they do.
- **Always `clean`.** A stale `target/` produces false test *failures* (mtime-newer
  stale classes) **and** false test *counts* (Surefire never deletes reports it did
  not write, so leftovers from deleted test classes get summed in, silently and in
  the flattering direction). The previous pass published 1298/225 for this reason;
  the real number was 1198/210.
- **Measure in a scratch copy outside the repo**, because `target/` is sometimes
  left root-owned by the Docker build:
  ```
  rsync -a --delete --exclude target/ --exclude .git/ ./ /tmp/scratch/be/
  rm -rf /tmp/scratch/be/target/classes /tmp/scratch/be/target/test-classes
  ```
- **Cross-check the count**: test classes reported must equal the number of test
  classes in `src/test/java`. If they disagree, the measurement is unsound.
- **A "this is unused / removable" claim is produced by deleting it and running the
  suite**, never by reading (LAW §4). Report the before/after counts; matching
  counts rule out silent skipping as well as failure.

## Report findings in these categories

1. **DEAD CODE** — unreferenced classes, methods, ports with no implementation, use
   cases no `@Bean` wires, orphan request/response records, unused imports, config
   keys nothing reads, migrations nothing references, dev-seed rows for tables that
   no longer exist, commented-out blocks, unreachable branches. Anything that only
   *looks* dead (reflection, `@ConfigurationProperties` binding, Spring Data derived
   queries, Mustache template lookups, `PublicRouteRegistrar`/`RateLimitRuleRegistrar`
   SPI implementations, ArchUnit rules that scan by package) goes in "uncertain",
   separately.

2. **OVER-ENGINEERING** — abstractions with one implementation, config for things
   that never vary, wrapper layers that only pass through, patterns heavier than the
   problem. LAW §2.4's test is the bar: **name the caller that needs it.** Note that
   a *port* with one adapter is usually not over-engineering here — the application
   layer's allowlist is `com.vointika..` + `java..` and nothing else, so a port is
   often the only legal way to reach a library.

3. **UNDER-ENGINEERING** — copy-pasted logic that should be shared (quote both
   copies), missing error handling, unvalidated input, risky logic with no test,
   hardcoded values that should not be, things that break at 10x. **Include guard
   tests that pass vacuously** — see the invariants section.

4. **BAD PRACTICES** — security (secrets, injection, authz gaps), race conditions,
   swallowed errors, N+1 queries, shared mutable state, misuse of the framework's
   intended patterns. Boot 4 and Spring 7 differ from recall across the major
   boundary; verify against the pinned docs in `STACK.md`, never from memory.

5. **DRIFT AND CONTRADICTION** — where the code has diverged from itself or its docs. This repo's
   three-tier memory (LAW / MAP / repo docs) is the mechanism it relies on to
   survive session boundaries, so a stale line in `CLAUDE.md` or `PATTERNS.md` is a
   **high**-severity finding, not a cosmetic one: it sends the next session to
   reimplement something that exists or to trust a security claim that is false.
   Quote the doc line and the code line that contradict it, side by side. Check in
   particular: `STACK.md` pins vs `pom.xml`; `FlywayPerDomainConfig.DOMAINS` vs the
   migration folders; `CLAUDE.md`'s storefront route list vs the actual `@GetMapping`s;
   PATTERNS §4e's table vs the real translation tables; MAP's context table vs the
   endpoints that exist; half-finished renames (a type renamed but its parameters
   and test method names left behind — the compiler is satisfied and only a reader
   notices).

   **Hunt contradictions on purpose — do not wait to trip over one.** Every
   contradiction found in the 2026-08-15 pass was found by accident, while looking
   for something else. Run these four sweeps deliberately:

   - **Every number a doc states, check with a command.** Docs here count things
     constantly: "six tables", "four copies", "thirteen contexts", "eight
     addresses", "four registries", "15 list endpoints", "29 use cases". Grep the
     docs for number words and digits, list every count claimed, and verify each
     one. This is the highest-yield sweep in the whole audit: it is fully
     mechanical, and a wrong count is always a real defect — someone changed the
     thing and not the sentence. It is how `PATTERNS.md` §4e was caught saying
     **six** in its heading and **eight** in its body, nine lines apart.
   - **A heading against its own section.** Check the title of every section
     against what the section then says. A corrected body under a stale heading is
     the normal outcome of a hurried fix, and the heading is what a reader scanning
     for a recipe actually sees.
   - **Every "X is gone / deleted / does not exist / never existed" claim, grepped.**
     Docs and comments assert absence a lot. Grep for the named symbol. Both
     directions are findings: the thing still exists (the claim is false), or the
     thing is gone and something still references it (a stale pointer). It is how a
     `docker-compose.yml` comment was caught documenting a diagnostic deleted four
     days earlier.
   - **One fact, stated in two files.** LAW, MAP, `CLAUDE.md`, `PATTERNS.md` and
     `STACK.md` overlap deliberately. Pick the facts stated in more than one — route
     lists, context counts, which guard enforces what, what a rule requires — and
     diff them. When two disagree, **say which one the code supports**; do not just
     report that they differ.

   **A decision that was reversed is the sharpest case.** When a rule changes, the
   old rule usually survives in three or four places that nobody thought to grep.
   If you find a reversal, sweep every doc for the old rule and report each
   survivor, because each one will send a future session back to the reverted
   behaviour.

6. **COMMENT NOISE** — **recalibrated for this repo, and expect this category to be
   thin.** 54 section banners were swept in PR #154, and a restatement scan over
   15.5% comment density found exactly 3 candidates, all 3 of which turned out to
   disambiguate same-named constructors.
   Flag for DELETION: comments restating the code; section banners; commented-out
   code; generation narration; changelog/authorship comments; Javadoc that only
   repeats the signature; TODOs with no owner or actionable detail.
   **KEEP, and say so explicitly by name:** why-not-what, tradeoffs, rejected
   approaches, business rules; non-obvious workarounds citing a bug/spec; warnings
   about ordering or side effects; and — specific to this repo — **the heavy Javadoc
   on ports, security filters, interceptors, migrations and cross-context seams**,
   which is the recorded deliberate deviation from LAW §6.1 and is usually the only
   place a decision is written down. `EndpointRateLimitFilter`'s note on keying by
   matched pattern rather than concrete URI is the canonical keeper.
   The bar is unchanged: if deleting it loses no information a competent reader
   wouldn't get from the code, it goes. But a comment recording *why* the obvious
   alternative was rejected stays even when it is long.

7. **INVARIANT INTEGRITY** — the category the generic six miss, and where this
   codebase's real risk lives. For each, verify by reading the enforcing test and
   then **mutation-checking it**: break the invariant deliberately and confirm the
   build goes red and names the thing. A guard that passes vacuously is worse than
   no guard. This session has written three guards whose first version passed its
   own mutation.
   - **ArchUnit fences** — is `contexts_do_not_depend_on_each_other` still derived
     from the package structure (so a new context is fenced the day it appears)? Does
     the application-layer allowlist still admit only `com.vointika..` + `java..`?
     Note `javax..` is not `java..`.
   - **PATTERNS §4b** — does every list over tenant or growable data use the shared
     framework, cursor and all? Is every `.sortable(...)` field still a NOT NULL
     column (`SortableColumnsAreNeverNullableTest`)? Is `PAGE_SIZE` still read in
     exactly one place?
   - **PATTERNS §8b** — does every operator-facing mutation append to the audit
     trail **inside the same transaction**? Name any that do not.
   - **PATTERNS §4a** — `id` + `context`, never a prefixed id, never `type`.
   - **PATTERNS §4e** — does each translation overlay still fall back
     nullable-wins-canonical, and does the table actually match the doc's rows?
   - **PATTERNS §10** — does every seeded table still match its migrations? A
     migration that adds a required column to a seeded table aborts the whole seed
     under `ON_ERROR_STOP=1`, and every INSERT after it never runs. Nothing in the
     build catches this. This has bitten three times.
   - **PATTERNS §11** — is every public page route registered in all its places
     (`@GetMapping`, the `PublicRoute` GET *and* HEAD entries, and the lock
     interceptor's patterns)? Is every path variable in a `PublicRoute` pattern
     constrained? An unconstrained `/{locale}` `permitAll`s every single-segment
     path in the application.
   - **Never store URLs** — storage key on the row, resolved at read time.

## Verification — no finding goes in the report unverified

Every claim must be backed by something you actually ran or actually read, not by
what a codebase of this shape usually looks like. This is LAW §4 and it is absolute.

- **Dead code**: grep the whole repo for the symbol — including string literals,
  `application.yml`, Mustache templates, migrations, the dev seed, test fixtures and
  CI. A symbol is only dead if the search comes back empty **and** deleting it leaves
  the suite green. Show the search you ran and the two suite counts.
- **Unused deps**: check imports, `pom.xml` scope, build plugins and transitive use
  before calling one unused. Some are load-bearing without being imported.
- **Bugs and races**: trace the actual call path. Write the test if you can.
- **Duplication**: quote both copies side by side. "Similar" is not duplication.
- **Doc drift**: quote the doc line and the contradicting code line.
- **Security**: confirm the input is genuinely reachable from an untrusted source and
  trace it from the entry point. On this app that means: the storefront's host-derived
  tenant, the four unauthenticated route groups, and the request body — everything
  under `/api/tour-operators/{id}/**` is behind the membership interceptor.
- **Behaviour**: run it, or read the implementation end to end. Never infer behaviour
  from a name.
- **Version-specific behaviour**: `STACK.md` → the pinned version's official docs.
  Recall across a major-version boundary is not a source and has cost this project
  real time.

If you cannot verify something: dig further, or put it in **UNVERIFIED — needs human
check** with an explicit note on what you could not confirm and why. Never state an
unverified claim as fact, and do not soften by hedging in the prose. A finding is
either verified and asserted, or it is in the unverified section. Nothing in between.

## For each finding

- `file:line`
- severity (critical / high / medium / low) — calibrated for this project:
  - **critical**: exploitable by an unauthenticated request, or silent data loss.
  - **high**: a cross-tenant or authz gap behind auth; a broken invariant from
    category 7; **a stale line in LAW/MAP/`CLAUDE.md`/`PATTERNS.md`/`STACK.md` that
    would send the next session down a wrong path.**
  - **medium**: a real defect with a bounded blast radius.
  - **low**: breaks at 10x, or is a standing hazard with no reachable path today.
- tag: NEW / KNOWN-OPEN / REGRESSION / STALE-RECORD
- what's wrong, in one or two sentences
- the suggested fix and roughly what it costs
- **Verified by**: the specific command, file read, or test run that confirms it

For category 6, group by file, give a count, quote a few representatives, and list
the keepers by name.

## How the report must read

Write it for a working engineer skimming on a phone. Direct, plain, no performance.

- **Say the thing first.** Every finding opens with what is wrong, in one sentence,
  in ordinary words. Background comes after, if it is needed at all.
- **No jargon, no academic register.** Not "this constitutes a latent invariant
  violation" — "a locked store serves this page to anyone". If a term has a plain
  equivalent, use the plain one. Keep the precise word only when it *is* the
  precise word: `keyset cursor`, `@GetMapping`, `NOT NULL`. Never use a long word to
  sound careful.
- **Short sentences. Active voice.** "The guard misses it", not "it is not detected
  by the guard".
- **No hedging.** Not "this may potentially cause issues under certain
  circumstances" — either it does and you say so with the proof, or it goes in
  UNVERIFIED. "Might", "could arguably", "it seems", "somewhat" are all banned in
  the findings; they belong nowhere in a report where every claim is supposed to be
  verified.
- **No throat-clearing.** Cut "it is worth noting that", "one thing to consider",
  "as previously mentioned", "in order to". Delete the sentence that introduces the
  point and keep the point.
- **Quantities, not adjectives.** "1200 tests pass with the route ungated", not
  "the test coverage here is somewhat weak".
- **Say what it costs and who it hurts.** A finding a reader cannot act on is not
  finished. "A locked store serves it to an anonymous visitor" tells them why to
  care; "violates the gating invariant" does not.
- **Own the mistakes plainly.** If a scan of yours was wrong, say so in one line and
  move on. No apologising, no dwelling.

The test: read a finding aloud. If it sounds like a person explaining a bug to a
colleague, it is right. If it sounds like a paper, rewrite it.

## Rules

- Don't pad. A short accurate report beats an exhaustive speculative one.
- Distinguish "objectively broken" from "a style preference of mine".
- An empty category is a fine result — say "none found" rather than inventing
  something. Several categories here have legitimately come back empty eight passes
  running.
- Stay in scope (LAW §6.4): report, do not tidy the neighbourhood.
- End with the 5 things you'd fix first, in order, and why.

## Output

Write the report to `AUDIT.md`, **replacing** the current one — do not create a
dated file beside it; git holds the history. Note in the header which pass it
supersedes and what that pass's findings became.

**`AUDIT.md` is tracked** (policy set 2026-08-15, reversing PR #160). Commit it on
the branch that produced the audit. The earlier rule kept it out on the grounds that
a point-in-time report is not a governing doc — the premise still holds, but two
reports were lost that way, each after being read by exactly one session.

**Stage explicit paths anyway**: `git add -- AUDIT.md src ...`, **never
`git add -A`**. That caution was doing double duty and only half of it is gone — a
`git add -A` is what swept a report onto `main` by accident in #155, and it will do
the same to the next scratch file.

The durable half of anything worth keeping still belongs in `../MAP.md`, not in the
report. A report is a dated snapshot; MAP is what crosses session boundaries.
