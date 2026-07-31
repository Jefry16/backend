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

Both `touroperator` and `identity` came back with **zero** genuinely dead members.
That is the expected result, and it is only worth anything if the examined counts
are printed beside it.

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
  differed (the input adds the caller and the path ids) while the tree node inside
  them was identical — so a pair-level diff passes and a recursive copy runs on
  every save. The nested type is where the cost is, because collapsing it deletes a
  mapper, not just a file.
- **A guard against a state the framework prevents is dead code.** A required
  `@RequestBody` — the default — rejects an absent body *and* a literal `null` with
  400 before the handler runs, so `body == null ? null : body.x()` never fires.
  Only `@RequestBody(required = false)` makes it live. This one is settled by a
  probe test in thirty seconds; reading the annotation is what gets it wrong.

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

**A guard inside a state transition defends that transition and nothing else.**
`experience` enforced "a cancelled slot is terminal" inside `Slot.changeStatus`, so
the capacity-only `PATCH` — which never calls it — edited cancelled slots and audited
the result. The fix is to ask the invariant **once, where the edit begins**
(`slot.ensureEditable()` at the top of the use case), and let the transition method
reuse it; a path added later then inherits the guard instead of having to remember it.
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
timeout 590 ./mvnw -o test -Dtest=ArchitectureTest 2>&1 | grep -cE "was violated"
cp /tmp/probe.bak "$F"
```

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
The api-guide does not document every endpoint (MAP Debt), so the test is the only
place a client's assumption is recorded.

Then, in the same pass (LAW §3): `CLAUDE.md` if a claim it makes changed,
`PATTERNS.md` if a shape repeated twice, `../MAP.md` ledger and Debt, and delete any
Debt entry the pass paid off. A doc that contradicts the code is worse than no doc.

Commit per LAW §6.2 — subject says what changed, body only for what the diff cannot
show. Merge `--no-ff` (this repo is 30/30 merge commits), delete the branch, push.

## Tooling traps on this machine

- `grep -m` is unsupported here; `(public |)` is an invalid empty alternation.
- Paths beginning `-` are eaten as flags by `tar`, `ls`, `grep` — prefix with `./`.
- `git checkout <file>` silently discards **uncommitted** edits to that file.
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
