# Codebase audit — `vointika/backend` — 2026-08-15 (third pass)

Investigation only. No code changed.

Replaces the second pass (in git at `7ecf6ff`).

**Why rerun.** The first two passes leaned on doc sweeps and mechanical scans. The
categories that need someone to *read code* — over-engineering, under-engineering,
bad practices — kept coming back "none found" on the strength of greps. This pass
read the shared list framework end to end, which is what the playbook says to do.

It found a real bug. One finding below is new and live; everything else is carried
forward unchanged and gets one line, not a re-argument.

Tags: **NEW** · **KNOWN-OPEN** · **REGRESSION** · **STALE-RECORD**.

---

## Baseline

**1201 tests across 213 classes, 0 failures** — 213 matches the number of test files
on disk, so nothing is silently skipped.

Run as `./mvnw -o clean test -Dtest='!VointikaApplicationTests'` (1200 green) in a
clean scratch copy, then `contextLoads` separately once Postgres was available: **1
test, 0 failures**. Flyway validated all eleven domains in order and `ddl-auto:
validate` passed against the current entities — both previously unverified.

---

## 1. A "not equal" filter silently hides every row where the column is null

- **Files**: `shared/infrastructure/list/CriteriaListExecutor.java:129, 131, 142`
- **Severity**: **medium**, and it is the kind that gets found by a customer, not a test
- **Tag**: **NEW**

Three filter operators are built as a plain negation:

```java
case NEQ          -> cb.notLike(lowerCol, escaped, '\\');            // :129
case NOT_CONTAINS -> cb.notLike(lowerCol, "%" + escaped + "%", '\\'); // :131
return f.op() == FilterOp.NOT_IN ? cb.not(inPredicate) : inPredicate; // :142
```

In SQL, `NOT (NULL LIKE 'x')` is **unknown**, not true, and `WHERE` keeps only true
rows. So a row whose column is null matches neither the filter nor its negation. Ask
for "everything not named Tom" and you do not get the rows with no name.

**This is the same three-valued-logic trap the repo already documented and guarded
for sorting** (`SortableColumnsAreNeverNullableTest`). Nobody carried it across to
filters.

**Three fields are exposed today.** Derived, not guessed — the endpoint↔entity
pairing comes from the `listExecutor.list(...)` call, the same trick the sortable
guard uses:

| Endpoint | Field | Type | Negative op | Nulls occur? |
|---|---|---|---|---|
| `ListContactMessagesUseCase` | `name` | TEXT | `neq`, `not_contains` | **Yes, today** |
| `ListAuditLogUseCase` | `actorName` | TEXT | `neq`, `not_contains` | **Yes, reachable** |
| `ListAuditLogUseCase` | `actorId` | SET | `not_in` | Latent |

**The contact one reproduces on any dev database**, because the seed puts a null in
it (`dev-seed.sql:1213` — `(:'cm_gift_id', :'operator_id', NULL, 'ana@example.net', …)`):

```
GET /api/tour-operators/{id}/contact-messages?filter[name][neq]=Tom Baker
```

returns two of the three seeded messages. The third is not named Tom Baker and
should be there.

**The audit one is reachable and matters more.** `AuditTrailPortImpl:60-66` resolves
the actor's display name and ends `.orElse(null)`, so any USER actor whose account
lookup misses — a deleted user — writes a null `actor_name`. The migration says so
itself (`audit/V1:17`: *"frozen display name; NULL for SYSTEM or a name-less
account"*). An operator asking "what did everyone except Alice do" gets an audit
trail that quietly under-reports. That is the one place in this system where a short
list is worst.

`actorId` is latent, not live: `audit/V1:29` has a CHECK tying a null `actor_id` to
`actor_type = 'SYSTEM'`, and SYSTEM has no emitter yet.

**Nothing tests this.** No test in the repo exercises a negative filter against a
null column (`grep` over `src/test` for `neq|not_in|NOT_CONTAINS|notLike` combined
with `null` returns nothing).

- **Fix**: OR in an `IS NULL` for the three negative ops — `cb.or(cb.isNull(path),
  cb.notLike(...))` and the same for `NOT_IN`. That makes "not X" mean what a caller
  reads it to mean. Roughly an hour including tests. Decide it deliberately, though:
  the alternative is to keep SQL's semantics and *document* them, which is defensible
  but is not what the API currently claims anywhere.
- **Verified by**: `CriteriaListExecutor` read end to end; `FilterType.java` confirms
  TEXT allows `NEQ`/`NOT_CONTAINS` and SET allows `NOT_IN`; a script that pairs all
  15 schemas to their entities and resolves every filterable field's nullability
  through the `@Column` annotations and the `extends` chain → the three rows above;
  `audit/V1__create_audit_log.sql:16,17,29` and `contact/V1:14` confirm the columns
  are nullable in the database, not just in JPA; `AuditTrailPortImpl:60-66` for the
  `.orElse(null)`; `dev-seed.sql:1213` for the live null.
- **Executed against the running stack. It reproduces.** Twelve seeded contact
  messages, one with a null name:

  ```
  no filter                       -> 12   (null-name row present)
  filter[name][neq]=Tom Baker     -> 10   (expected 11)
  filter[name][not_contains]=zzz  -> 11   (expected 12)
  ```

  `not_contains=zzz` is the clean one: **no name contains "zzz", so the filter
  excludes nothing — and a row still disappears.** There is no reading of that
  result where the behaviour is intended.

  The audit trail's first page has no null `actorName` today, so the effect is not
  visible there yet. Same executor, same predicate, and `AuditTrailPortImpl:66`
  can write the null — it is waiting for one deleted user.

## 2. `PATTERNS.md` tells the next developer this is safe

- **File**: `PATTERNS.md:377-378`
- **Severity**: medium · **Tag**: **NEW**

> A nullable column is fine to *filter* on (not matching is the expected answer
> there); it is only sorting that breaks.

True for positive operators. Wrong for `NEQ`, `NOT_IN` and `NOT_CONTAINS`, where
"not matching" is exactly what the caller asked for and the null rows are dropped
anyway.

This sentence landed in #162 alongside the sortable guard — written while thinking
about sorting, and it over-claims by one word. It is the sentence someone will read
before adding the next filter.

- **Fix**: one clause. "Fine to filter on with a positive operator; a negative one
  (`neq`, `not_in`, `not_contains`) drops the null rows too."
- **Verified by**: the passage quoted against `CriteriaListExecutor:129,131,142`.

---

## 3. Carried forward — unchanged, one line each

All verified still true this pass. Full write-ups are in the second pass (`7ecf6ff`).

- **KNOWN-OPEN, high** — `PATTERNS.md` says the storefront password gate is deleted
  in three places (`:892-893`, `:924`, `:693`); it came back in #138. §11 is the
  recipe read before adding a storefront route, and it tells the reader not to gate
  it.
- **KNOWN-OPEN, high** — a storefront route written as a literal path is invisible to
  the lock interceptor; the guard reflects over constants, never over `@GetMapping`.
  Proven by mutation earlier today: 1200 tests green with a public, ungated page route.
- **KNOWN-OPEN, medium** — `PATTERNS.md` §11 contradicts `CLAUDE.md:84` and MAP on how
  many places a route is registered. The code supports `CLAUDE.md`.
- **KNOWN-OPEN, medium** — `PATTERNS.md:458` heading says "six of them"; `:467` says
  "Eight tables do this".
- **KNOWN-OPEN, medium** — PR #163 is flagged in MAP's header but still has no ledger
  entry; four of its five open findings confirmed at
  `ApiGuideDocumentsEveryListFieldTest.java:258, :121, :69, :266`.
- **KNOWN-OPEN, low** — `docker-compose.yml:115` documents `?format=json`, deleted on
  2026-08-11, and now sits above an unrelated line.
- **KNOWN-OPEN, low** — `STACK.md:19-27` omits the `ses` service (`node:22.23.1-alpine`
  + `aws-ses-v2-local@2.10.0`).
- **STALE-RECORD, low** — MAP's unbounded-metafields debt names two owner types; there
  are three since #139.
- **KNOWN-OPEN, low** — `PATTERNS.md:703` files §4c between §8d and §9.
- **KNOWN-OPEN, low** — `GlobalExceptionHandler.java:83-86` reports any
  `IllegalArgumentException` as the client's fault. Still no reachable path.

---

## 4. Read closely this pass, and clean

Recording these by name so the next pass does not re-read them without cause.

**`CursorCodec`** — cursors are unsigned base64, so a client can forge one. It does
not matter, and I checked all three ways it could:

- The decoded `sortField` is compared against the schema-validated sort at `:46`, so
  a forged field name never reaches `root.get()`.
- The decoded value goes through `ValueCoercion` and lands as a **bound parameter** in
  `cb.equal`/`greaterThan`, never string-concatenated. No injection.
- The tenant predicate is added independently at `CriteriaListExecutor:53-55`, so a
  forged cursor cannot reach another operator's rows.

What a forged cursor buys you is a jump to an arbitrary point in your own list.
Nothing.

I also checked one thing that would have been a quiet bug: `decode` catches
`IllegalArgumentException` at `:50`, and it throws `InvalidFieldException` at `:47`.
If the latter extended the former, the specific "Cursor does not match the requested
sort" message would be swallowed and replaced with "Invalid cursor". It does not —
`InvalidFieldException extends DomainException extends RuntimeException` — and
`CursorCodecTest:46,55` asserts the message survives.

**`CriteriaListExecutor` ordering** — the `ORDER BY` tie-break at `:71-75` uses the
same direction as the primary sort, and the cursor predicate at `:189-201` matches it
(`lessThan` for DESC, `greaterThan` for ASC). They agree, which is what keyset
pagination needs and what silently corrupts a page when it is wrong.

**LIKE escaping** — `:164-166` escapes `\`, `%` and `_` before every `LIKE`, in that
order, so a filter value of `100%` is a literal.

---

## 5. Everything else — unchanged from the second pass

Dead code: **none**. Zero unused imports, zero unwired use cases, zero unimplemented
ports, zero `TODO`, zero unread config keys. The 66 classes referenced nowhere are all
Spring beans wired by type.

Invariants: the two ArchUnit fences fire when broken and name the file. Seed matches
migrations across 34 inserts and 42 tables. All 72 auditing use cases append inside
the transaction. The one gap is the route guard, above.

Comment noise: 18% density, which is the deliberate calibration. Four banners in
`GlobalExceptionHandler`, one of them informationless. Zero commented-out code, zero
authorship comments, zero narration.

---

## UNVERIFIED

Two items closed against the running stack after the report was first written; what
is left is below.

- **The literal-route leak against a real locked store** — proven against the test
  suite (1200 green with a public, ungated page route), not observed over HTTP. The
  seeded operator's gate would have to be enabled to watch it.
- **`ReplaceMenuItemsUseCase` at scale** — no item cap, verified by reading. The
  actual query count for a large tree is still unmeasured.
- **`actorName`'s null case in production** — the code path is proven
  (`AuditTrailPortImpl:66` returns `.orElse(null)`) and the filter bug is proven on
  `contact_messages`, but no audit row in dev has a null actor name, so the two have
  not been observed together.

### Closed

- **Finding 1 — executed, reproduces.** Numbers in the finding above.
- **Flyway ordering and `ddl-auto: validate`** — `VointikaApplicationTests.contextLoads`
  run against the live database: **1 test, 0 failures, BUILD SUCCESS.** All eleven
  domains validated in order (2, 7, 16, 3, 4, 2, 14, 4, 8, 2 … migrations per schema),
  and Hibernate's `validate` passed against the current entities. The full suite is
  therefore **1201 green**, not 1200 with an excluded test.

---

## Fix these five, in order

1. **Finding 1 — decide what a negative filter means when the column is null.** It is
   the only live code defect in three passes, it reproduces on the running stack, and
   it is about an hour. The audit trail is the wrong place to under-report.

2. **The three stale gate sentences in `PATTERNS.md`.** Twenty minutes. First among
   the carried items because §11 is read *before* someone adds a storefront route, and
   it currently tells them not to gate it.

3. **Make the route guard read mappings, not constants.** The only finding with a
   security consequence. Together with (2) it closes both halves of one hazard — the
   recipe says you need not gate the route, and the build agrees.

4. **Finding 2 and the §4e heading — two sentences in `PATTERNS.md`.** Both are cases
   where a correct fix left a wrong sentence behind, which is how a trusted doc starts
   lying.

5. **Land #163 in MAP, and the three one-line doc fixes.** Half an hour for all of it.

---

*Audit reports are tracked (policy set 2026-08-15, reversing PR #160). A report is a
dated snapshot; `../MAP.md` is where anything durable lives. Superseding a report
replaces this file — git holds the history. The playbook is `REPO-AUDIT.md`.*
