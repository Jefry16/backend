# Context audit — the playbook

Run this against one bounded context. Invoke with nothing but the name:

> **Run `CONTEXT-AUDIT.md` on `page`.**

It was written from the `identity` pass (2026-07-31) and every command in it was
executed on this machine. It is deliberately specific about the ways the checks
lie, because most of that pass was spent discovering them.

## Before anything

1. Read LAW (`../CONSTITUTION.md`), `../MAP.md`, `CLAUDE.md`, `PATTERNS.md`, `STACK.md`.
2. `git checkout -b audit/<context>` — trunk takes no direct commits.
3. Baseline the suite: `./mvnw -o test` and record the number. Every later count is
   compared to it, and a drop must be explained, not assumed benign.
4. **Grep MAP's Debt section for the context's name first.** Earlier passes log what
   they found and deliberately left; `audience` arrived with its entire finding list
   already written there by the `touroperator` pass. Start from those and delete each
   entry as you pay it off.

## The rule that governs the whole pass

**A claim about behaviour is produced by running something, never by reading.**
"Nothing references it" and "nothing breaks without it" are different questions.
Delete it, run the suite, put it back — that is the entire method.

**And the probe is not the deliverable — the test is.** A throwaway probe proves the
behaviour today; it leaves nothing behind to notice the day it changes. Every pass so
far has changed something API-visible on the strength of a probe that was then
deleted: two null-guard removals resting on `@RequestBody` being required by default
(`touroperator`), and a 422 → 409 on an already-cancelled slot (`experience`). Both
were right, both were disclosed, neither was pinned. **If a probe justified a change,
the probe becomes a committed test before the PR opens** — and mutation-check that
test too, or it may be passing for reasons unrelated to what it claims to guard.

## 1 · Measure

```bash
C=<context>
find src/main/java/com/vointika/$C -name '*.java' | wc -l
find src/main/java/com/vointika/$C -name '*.java' -exec cat {} + | wc -l
grep -rhoE "@(Get|Post|Put|Patch|Delete)Mapping" src/main/java/com/vointika/$C | wc -l
```

Compare **files per endpoint** and **LOC per file** against the other contexts, not
against a target. `identity` ran 6.4 files/endpoint and 34 LOC/file — mid-pack on
lines, heaviest on files. That shape means ceremony, not verbosity, and it points at
DTO layers and the domain/JPA double model.

**The ratio is a pointer, not a verdict.** `reference` runs 10.6 files/endpoint, the
highest anywhere, and every file is earned: three aggregates × the six-file
persistence recipe (§3), serving one read endpoint each, plus §4's nested-only
variant for `Country`. A context with N tables and N read endpoints cannot score
well on a per-endpoint metric and nothing is wrong with it. Follow the ratio to the
file list, then say which recipe explains it — or which does not.

### If the target has no endpoints

`shared` and `notification` have none, so the ratios above measure nothing and §4's
application allowlist does not apply either — the kernel must depend on *nothing*,
which is the inverse of the check. Substitute:

**Every port must name a live caller.** For each interface in `shared/port`, list the
contexts that reference it and the classes that implement it.

```bash
for p in $(ls src/main/java/com/vointika/shared/port/*.java | xargs -n1 basename | sed 's/\.java//'); do
  impls=$(grep -rl "implements .*\b$p\b" src/main/java --include="*.java" | wc -l)
  ctxs=$(grep -rl "\b$p\b" src/main/java/com/vointika --include="*.java" \
         | sed 's|src/main/java/com/vointika/||;s|/.*||' | sort -u | tr '\n' ' ')
  printf "%-34s impls:%s  used by: %s\n" "$p" "$impls" "$ctxs"
done
```

Expect one implementation and at least two consumers — **for the interfaces**.
`shared/port` also holds the `*View` records and `NewAuditEntry` that those
interfaces carry, and those correctly report `impls:0`; four of the twenty-nine
files are records, not ports. Filter on `interface` before reading the column as a
finding.

Anything else is a question, not necessarily a fault — `AccessTokenValidatorPort` has a single consumer because the
kernel's own `JwtAuthenticationFilter` is the caller, which is correct. **Name every
exception you find; an acknowledged gap left unspecified reads as a lurking problem.**

Then go a level finer, because a port can be live while one of its *methods* is not.
That is how `UserAccountQuery.findAccounts` survived: the port was in use, so a
port-level check passed, and only reading the one call site showed it being handed a
single-element set to do what the sibling method already did.

## 2 · Dead code

Scan classes, interface methods, entity methods, record components, enum constants.
**Print how many members each scan examined.** A scan that finds nothing and a scan
whose pattern is broken produce identical output; the count is the only difference.

Seven false positives that cost real time, all of which will recur:

- **Spring beans have no name references.** `@Repository`, `@Component`,
  `@RestController` and SPI registrars are found by annotation. Fifteen classes in
  `identity` looked dead and were not.
- **Private helpers are called without a dot.** A `\.method\(` pattern misses
  `extractClaims(token)` inside the same class.
- **Records have implicit accessors.** A "public methods" scan sees nothing in a
  `record`; read the component list out of the declaration instead.
- **Lombok fields are read through a generated getter.** A scan matching a JPA
  entity's field *name* finds every one of them unused — 59 of `touroperator`'s
  60 flagged fields were this. Match `getX(`/`isX(` instead, and every field
  resolved.
- **The §4a `context` component has no caller at all.** Only Jackson reads it, so
  it is dead by every static measure and load-bearing on the wire. Same for any
  response field the API contract carries but no Java code re-reads.
- **An implemented interface method never looks dead to a declaration count.**
  If the check is "occurrences ≤ declarations", the `Impl`'s own signature is a
  second declaration in a second file, so the method scores as referenced.
  Count **dotted call sites** (`\.name\(`, `::name`) instead — and keep the
  bare-name pass too, for the private helpers above. Run both; the union is sound.
- **`typeof null === 'object'`-class errors.** Assert the shape you mean.

**A Javadoc naming a caller is not evidence the caller exists.**
`UserAccountQuery.findAccounts` announced itself as "the roster's N+1-free
enrichment path"; the roster never called it, and the one caller it did have
passed a single-element set to a batch API. The doc was written when the method
was, and nothing re-checked it when the roster went denormalized instead. Grep the
named caller before believing the sentence — this is cheap and it is how a whole
orphaned branch stays plausible for months.

Fourteen passes have run, covering every context and `shared`, and **every one came
back with zero genuinely dead members.**
That is the expected result, and it is only worth anything if the examined counts
are printed beside it. The subtractions that did land came from §3 and §5, not here:
duplicate DTOs, an unreachable guard, a port method with no caller.

## 3 · Over-engineering

The test is LAW §2.4: **name the caller that needs it.** Applied honestly, most
ceremony survives:

- A **port with one implementation** is not waste if the tests mock it. Check:
  `grep -rl "@Mock.*<Port>" src/test | wc -l`. In `identity` the four local ports were
  mocked in 2–8 test classes each — the caller is the test.
- The **domain/JPA double model** is expensive (574 LOC in `identity`, 293 of it pure
  mapping) and *bought*, by the rule that domain stays free of JPA. Priced, not wasted.
- **Identical DTO pairs are the real find — run this check first.** It has paid out in
  every context so far: four pairs in `identity`, a nested node in `touroperator`,
  three more in `experience`. Compare each `presentation/request/XRequest`
  to `application/dto/input/XInput` field by field. Where identical, the second
  insulates nothing. Collapse per PATTERNS §4c — the **application** record survives,
  the controller binds to it, and the wire contract is diffed before deleting.
  **Compare the nested records too.** In `touroperator` the two wrappers genuinely
  differed: the input adds the caller and the path ids. But the tree node inside
  them was identical, so a pair-level diff passes while a recursive copy runs on
  every save. The nested type is where the cost is, because collapsing it deletes a
  mapper, not just a file.
- **A guard against a state the framework prevents is dead code.** A required
  `@RequestBody` — the default — rejects an absent body *and* a literal `null` with
  400 before the handler runs, so `body == null ? null : body.x()` never fires.
  Only `@RequestBody(required = false)` makes it live. This one is settled by a
  probe test in thirty seconds; reading the annotation is what gets it wrong.

**Config outlives the feature it was sized for.** `spring.servlet.multipart.max-file-size`
was 510MB, set in the first commit when the only upload was a 5 MB avatar. The largest
cap today is 25 MB, and the container spools the part before any handler runs. Nothing
was wrong at the time, and nothing announced that it had become wrong. For each limit,
timeout and pool size the context relies on, `git log -S` the value and ask what it was
sized against — then pin it to the thing it must track, the way
`MultipartLimitsTest` and `TemplateLocalesTrackUiLanguagesTest` do. Two numbers that
must agree are the most common finding in this whole pass.

**A doc that promises how cheap a future change will be is a must-agree pair with no
second number.** `AuditActorType` said adding `STOREFRONT` was "a one-line
CHECK-constraint migration" — plausible, uncheckable, and wrong twice over: nothing
made the enum and `audit_log_actor_type_check` agree, and the invariant that a
non-`USER` actor carries no id lives in two more places the sentence never mentioned.
Treat "adding X is just Y" in a comment as a claim to verify, then either enforce it
or correct it. **Prefer pinning the pair where drift is silent.** `audit`'s four
column widths also mirror the DDL, and those were left alone on purpose: over-long
input is rejected loudly by whichever side is stricter, whereas the enum gap only
surfaces in production, on the first write by the new actor type — and takes the
audited action down with it.

**A use-case test cannot see which query the adapter chose.** Use-case tests stub the
domain repository, so everything decided *below* that port is invisible to them —
including whether the duplicate-name pre-check calls `existsBy…IgnoreCase` or the
case-sensitive sibling. In `pickup` all ten use-case tests stayed green while the
pre-check was made case-sensitive, which is the bug `audience/V2` exists to fix.
When an invariant lives in the adapter's *choice of query*, the test belongs on the
adapter. Suspect this wherever a domain method name is vaguer than what it does
(`existsByTourOperatorIdAndName` that ignores case).

**A removal can also come back clean, and that is worth reporting.** `reference` dropped
its `/countries` endpoint and left no orphan repository, use case or route — the exact
check that found rot in `pickup` found nothing here. Run it both ways and say which
answer you got; "audited the removal, nothing left behind" is a result.

**When the context's history includes a removal, audit the removal.** Migrations are
immutable, so a dropped feature leaves a create-then-drop pair on purpose — that part
is correct. What rots is everything that *explained* it: `FlywayPerDomainConfig` still
justified pickup's ordering by a slot↔pickup snapshot deleted in `experience/V6`.
Grep the removed feature's nouns across the whole repo, not just the context, and read
what the hits are asserting.

**Check the `ListSchema` against what the screen is for.** A list can use the shared
framework correctly and still be unusable. `ListSlotsUseCase` makes `startAt`
sortable and **not filterable**, and `ListSchema` has no `LocalDateTime` builder — so
"slots in August" cannot be asked for, which is the single most obvious filter on a
departures list. Client-side filtering of the current page looks like a workaround
and is not one, because the rest is behind the cursor. For each list, name the first
filter its screen would offer and check the schema can express it.

### Auditing a worker module (`notification`)

No endpoints, no domain, no presentation — §1's ratios measure nothing, but §4's
application allowlist *does* apply (unlike `shared`). Substitute:

- **Every consumer has a producer, and every produced event has a consumer.**
  Cross `EventTopics.BY_EVENT_TYPE` against `new <Event>(` call sites and
  `@KafkaListener` topics; the healthy shape is 1 publisher : 1 consumer per event.
- **Every asset the module ships is declared, and every declared asset exists.**
  Templates are the `notification` case: the catalog fails fast at boot on a
  missing file, so the gap that survives is the *other* direction — an asset set
  that must track a config allowlist somewhere else.
- **Every consumer log-and-swallows** (PATTERNS §7), or one bad record stalls a
  partition.

**Hunt for a second list that must agree with a config allowlist.** The catalog's
locales "should track" `app.identity.ui-languages` — a comment, enforcing nothing,
while three docs promised growing a language was code-free. The failure mode is the
dangerous kind: not an error but a **silent downgrade** (the email sends, in the
wrong language). Whenever a fallback exists, ask what it hides.

### Auditing `shared` instead of a context

The kernel has no endpoints and no application layer, so §1's ratios and §4's
allowlist do not apply. Two checks replace them:

- **Every port names a live caller.** For each `shared/port/*`, count
  implementations and consuming *contexts*. One implementation and ≥2 contexts is
  the healthy shape. **One implementation and one context means the seam is not a
  seam.** A port method whose only caller degenerates its own signature — a
  single-element set passed into a batch API — is the same finding one level down.
- **Every framework extension point is a false positive.** `@Bean` factories,
  `@ExceptionHandler` methods, `doFilterInternal`, `WebMvcConfigurer` and
  `RecordInterceptor` overrides are all invoked reflectively — in `shared` they
  were 33 of 33 flagged methods. Check each exception class has both a thrower
  and a `GlobalExceptionHandler` mapping; the base class and any exception the
  runner *constructs* rather than throws are the legitimate zero-thrower cases.

## 4 · Coupling

```bash
grep -rhoE "^import (static )?[a-z][A-Za-z0-9_.]*" --include="*.java" \
  src/main/java/com/vointika/$C/application | sed 's/^import \(static \)\?//' \
  | grep -vE "^(com\.vointika|java\.|javax\.)" | sort -u
```

The allowlist is `com.vointika..` + `java..`. Anything else is a missing port.
Do **not** trust a blocklist grep: Jackson 3 lives under `tools.jackson`, and a ban
naming `com.fasterxml` sails past it. ArchUnit reads the dependency graph and finds
what grep spells wrong.

## 5 · Fix at the seam, not the symptom

Ask where the failure actually *surfaces*.

JPA flushes at **commit**, so a lost unique race comes out of
`transactionRunner.run(...)` — not `repository.save(...)`. Translating in the
repository adapters would have caught nothing. One translator in
`SpringTransactionRunner` fixed 21 use cases across 8 contexts.

Same shape elsewhere: a best-effort side effect belongs to the **adapter**, not the
caller. If every caller of `port.deleteObject` wraps it in try/catch, "best effort" is
the port's contract — move the swallow into the adapter and document it on the port.

When you narrow a catch, ask what the broad one was absorbing. Those 21 caught the
*parent* class, so foreign-key and not-null failures were being handled as races — in
registration that could return a fake success and email a stranger.

**Read every `catch` against what the translator actually throws.** A catch names the
exception it expects, and nothing checks that anything ever produces it —
`DeleteMetaobjectDefinitionUseCase` caught `UniqueConstraintViolationException` for a
**foreign-key** violation, so the 409 it documented was a 500. Postgres raises 23505
for unique (→ `DuplicateKeyException`, the one class `SpringTransactionRunner`
translates) and 23503/23502/23514 for FK/not-null/check (→ the untranslated parent).
Grep every `catch (UniqueConstraintViolationException)` and read its comment: if the
stated reason is not a unique index, it cannot fire. Settle it by running Spring's
`SQLExceptionSubclassTranslator` over the SQLSTATEs — thirty seconds, and it beats
reasoning about a hierarchy.

**When a read path consults two sources in precedence order, every write that feeds
either must check both.** `page` resolved a storefront handle against localized
handles first and canonical ones second. But create and rename validated only against
`pages`, and the translation upsert only against `page_translations`. Each write
looked complete in isolation. Together they let one page shadow another and vanish
from a locale. The tell is a `.or(...)` / fallback chain in a read adapter — follow it back
and list every writer of each branch. Recipe in PATTERNS §4d.

**"Every writer" means every path that *produces* the value, not every path that
*accepts* one.** The `page` pass predicted the same defect in `experience` and filed it
as Debt, correctly. But it sized it as one write path, reasoning that experience handles
are immutable so there is no rename to guard. True, and beside the point: the create path
*generated* a canonical handle while probing only its own table, so generation was a
second door. Immutability rules out later edits. It says nothing about the write that
sets the value in the first place. When you enumerate writers, list the derivers
alongside the setters.

**A predicted defect is worth filing even when it costs a second pass.** The `page`
audit found the twin in `experience`, verified it, and left it. In scope terms that was
right, and the entry is what made the fix cheap two weeks later. What the entry could not
be trusted as is a *scope estimate*. It was written from the outside, and the implementing
pass still had to enumerate the write paths itself. File the finding, not
the plan.

**A guard inside a state transition defends that transition and nothing else.**
`experience` enforced "a cancelled slot is terminal" inside `Slot.changeStatus`, so
the capacity-only `PATCH` — which never calls it — edited cancelled slots and audited
the result. The fix is to ask the invariant **once, where the edit begins** —
`slot.ensureEditable()` at the top of the use case — and let the transition method
reuse it. A path added later then inherits the guard instead of having to remember it.
Read every write path of an entity that has a terminal state and ask which of them
actually reaches the check. Where a use case already asks at the top —
`AcceptInvitationUseCase`'s status matrix — that is the shape to copy.

## 6 · Enforce, then break it on purpose

A finding you cannot re-detect is a finding you will make again — and that applies to
behaviour as much as to structure. **Where the invariant is expressible in ArchUnit,
add the rule; where it is not, add the test.** A cancelled slot staying uneditable, a
required body rejecting `null`: no type-and-package analysis can see either, and
"not mechanically detectable" is a reason to write a test, not a reason to write
nothing. Then **mutation-test whichever you added**:

```bash
cp "$F" /tmp/probe.bak                     # never `git checkout` uncommitted work
# add a FIELD of the forbidden type — not an import
grep -c "<the thing you just inserted>" "$F"   # PROVE the mutation landed
timeout 590 ./mvnw -o test -Dtest=ArchitectureTest 2>&1 | grep -cE "was violated"
cp /tmp/probe.bak "$F"
```

**Confirm the mutation applied before believing the result.** A `sed` that matches
nothing leaves the file untouched and the suite green. The conclusion is then "the
rule has a hole" or "their test is fake". Both are wrong, drawn from a command that
silently did nothing. It has happened on a wrong type name (`AudienceTranslationRequest` for
`UpsertAudienceTranslationRequest`) and on a wrong package. One `grep -c` between
the edit and the run costs nothing and removes the whole class of error.

**This applies to reviewing a pass as much as running one.** A reviewer
mutation-testing someone else's rule is one silent non-match away from calling a
sound test worthless.

**ArchUnit reads bytecode.** An unused import emits none, so an import-only probe
passes under any rule — I "proved" a hole that way and was wrong.

Prefer rules that **derive** their scope from the package structure over rules that
list what they know about. Five hand-written isolation rules named the four contexts
that existed when they were written; seven landed afterwards unfenced, and the five
only fenced against the original four.

Prefer an **allowlist**. Exempt by *depending class*, never by *depended-on type* — the
latter lets the pattern spread to an unlimited number of new classes while staying
green.

**Say so when a finding is not mechanically detectable, instead of inventing a rule.**
ArchUnit reasons about types and packages; it cannot see an identical record pair or a
ternary guarding an impossible null. `touroperator` produced two such findings and no
new rule, which is the honest answer — a rule that does not actually re-detect the
finding is worse than none, because it reads like coverage.

**Audit the rules themselves while you are in there.** The allowlist that seals the
application layer had a dead `String[]` beside it, still listing `org.slf4j..` — a
value the live rule contradicts and the previous pass's own ledger entry denies.
Nothing referenced it, so nothing failed; the next reader would simply have believed it.

## 7 · Land it

Gates: `./mvnw -o test` green, count explained against the baseline. **Every
API-visible change gets a line in the PR body and a test in the diff** — a status
code that moved, a body that is now rejected, a field that stopped being optional.
`ApiGuideDocumentsEveryEndpointTest` and `ApiGuideDocumentsEveryListFieldTest` both
fail the build on an undocumented endpoint or list field, so the guide is a gate
rather than a hope — but neither can see a *field table*, and 19 body-returning
endpoints still have none. Where a client's assumption is not in the guide, the test
is the only place it is recorded.

Then, in the same pass (LAW §3): `CLAUDE.md` if a claim it makes changed,
`PATTERNS.md` if a shape repeated twice, `../MAP.md` ledger and Debt, and delete any
Debt entry the pass paid off. A doc that contradicts the code is worse than no doc.

Commit per LAW §6.2 — subject says what changed, body only for what the diff cannot
show. Merge `--no-ff` (this repo is 30/30 merge commits), delete the branch, push.

## Tooling traps on this machine

- `grep -m` is unsupported here; `(public |)` is an invalid empty alternation.
- Paths beginning `-` are eaten as flags by `tar`, `ls`, `grep` — prefix with `./`.
- `git checkout <file>` silently discards **uncommitted** edits to that file.
- A stale `target/` produces false failures *and* false counts — the full trap and
  the scratch-copy recipe are in `CLAUDE.md`, which every session reads. The
  branch-switch symptom is a **`BUILD FAILURE` with no failing test in the output**.
- **Never probe by writing into `src/main/resources/db/migration/`.** `contextLoads`
  boots the real application, so Flyway **applies** whatever is sitting there to the dev
  database — a throwaway migration becomes a permanent row in
  `<schema>.flyway_schema_history`, and its DDL really runs. A probe that narrowed an
  audit CHECK constraint did exactly that, and the damage was invisible for a whole
  review round: the migrations applied cleanly, the suite went green, and it only
  surfaced later as a checksum mismatch when the probe was recreated with different
  content. Probe against a **scratch copy of the directory**, or accept that you are
  editing the database. Repairing it means restoring the DDL to what the real migration
  declares *and* deleting the probe rows from the history table.
- **Deleting a file under `src/main/resources` does not remove it from the build.**
  Maven copies resources into `target/classes` and never prunes; Flyway and every other
  classpath reader see the stale copy, so the deleted migration keeps running. Clear
  `target/classes/...` too, or the cleanup looks done and is not.
- Python string surgery fails silently. `assert old in s` on every replacement, or
  match on content rather than guessing indentation. Verify the file afterwards.
- Removing `@Component` from a class that gains a test constructor leaves Spring with
  two constructors — the injected one then needs `@Autowired`.
- Changing a port's contract breaks tests that stub it to throw. That behaviour did not
  disappear, it moved: move the test to the adapter rather than deleting it.

## Done looks like

Suite green and the delta explained · no dead code, with the examined counts shown ·
every finding either fixed or written into Debt with an owner · every new rule proven
by mutation both ways · docs true · branch merged, pushed, deleted.

State plainly what you did **not** do, and why.
