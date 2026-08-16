# API-docs sync audit — the rolling report

**Contexts done: `audit`, `contact` (2026-08-16).** Ten to go; `storefront` is last.
Playbook: `API-DOCS-SYNC.md`.

**One section per context, newest first.** Within each, findings A–H are what the
audit found *before* anything changed — the audit pass itself changes nothing, which
is the playbook's rule — and "What was fixed" says what is true now. Read the two
together.

**Every finding raised so far is fixed.**

Three results hold repo-wide and save every later pass the work:

- **No `relaxed*` documentation exists anywhere.** That is the whole of finding E,
  and it is closed for every context.
  **It does not mean every response is checked.** An operation that passes no
  `responseFields(...)` at all has no strict check either, and 19 of them publish a
  body with no field table — listed at the foot of this report. **Check field-table
  coverage in your context; only the `relaxed*` question is settled.**
- **Every `operation::` macro resolves and every snippet directory is pulled in**, so
  B, C and D are clean unless a pass breaks them.
- **The error shape is `status`, `error`, `message`, `code`, `timestamp`.** There is
  no `path` field, and it is `code`, not `errorCode`. `code` is
  `@JsonInclude(NON_NULL)`, so where the throw site supplies none it needs
  `.type(JsonFieldType.STRING)` as well as `.optional()`, or REST Docs cannot infer a
  type and fails the build. Copy `ERROR_FIELDS` from
  `ContactMessageControllerDocumentationTest`.

---

# `contact` — 2026-08-16

Three endpoints, all three documented, **no findings in A through E**. The whole gap
was errors: not one of the three published anything but its happy path.

## Endpoint table

| method | path | has test | has snippet | in the guide | strict or relaxed | status |
|---|---|---|---|---|---|---|
| GET | `/api/tour-operators/{tourOperatorId}/contact-messages` | yes | `contact-messages/list` | yes | strict | documented |
| GET | `…/contact-messages/{messageId}` | yes | `contact-messages/get` | yes | strict | documented |
| DELETE | `…/contact-messages/{messageId}` | yes | `contact-messages/delete` | yes | strict | documented |

*No guide line numbers in these tables. Every pass that inserts a section shifts the
ones below it, so they rot by construction and fail silently.*

**A–E: none.** Three mappings, three documenting tests, three snippet directories,
three `operation::` lines. `contact-messages/delete` has no `response-fields.adoc`
and that is correct, not a gap — it answers 204 with no body.

- **Verified by**: `ls target/generated-snippets/contact-messages/` → `delete`, `get`,
  `list`. `grep -n "operation::contact" src/docs/asciidoc/api-guide.adoc` → 1362,
  1369, 1377. `grep -rln "contact-messages\|ContactMessageController" src/test/java`
  → `ContactMessageControllerDocumentationTest`.

## F. Partial coverage — the whole finding

**F1. A STAFF caller can read a message and be refused when deleting it, and nothing
said so.** Reads are any-member; delete is ADMIN+ through `ensureAdmin`. A STAFF
member therefore lists the inbox, opens a message, and gets **403** with
`"This action requires ADMIN privileges"` on delete. That path was neither tested nor
documented.

- **Severity**: medium, and the highest-value finding in this context. It is a
  permission boundary *inside* one resource, so a client that tested with an ADMIN
  token will not discover it until a STAFF user does, in production.
- **Verified by**: `TourOperatorMembershipPolicy:48-55` throws
  `ForbiddenException`; `GlobalExceptionHandler:70-73` maps it to 403. No 403
  assertion existed anywhere in `ContactMessageControllerDocumentationTest`.

**F2. None of the three documented its 404, and there are three sites, not two.**
Both reads answer 404 for an unknown message and for another operator's — the lookup
is operator-scoped. **DELETE has the same 404** (`DeleteContactMessageUseCase:42`),
and it is the ordinary concurrent case: two admins with the inbox open, one deletes,
the other's DELETE misses. An earlier revision of this section counted two sites and
the first fix shipped only two; caught in review.

**F3. No `pathParameters` on any of the three.**

## G. Prose drift — none

The guide's filter and sort list matches `ListContactMessagesUseCase.SCHEMA` exactly
(`name`, `email`, `summary`, `createdAt`; sort `id` or `createdAt`). Its note that the
storefront intake does not exist yet is still true.

## H. Description quality — two, both fixed

`id` was described as "The message id" and `context` as `"contact-messages"` — the
first restates the name, the second restates the value. Both now say something the
field name does not.

## What was fixed

Three new tests, all documented, all rendering in the guide. (An earlier revision
of this line said two and pinned a suite count; the review round that followed added
the third and invalidated both — the same failure this section is about.)

- **F1** — `contact-messages/delete-forbidden` publishes the 403, under a guide
  heading that states the split: any member reads, ADMIN+ deletes.
- **F2** — `contact-messages/get-not-found` and `contact-messages/delete-not-found`
  publish both 404 sites.
- **F3** — `pathParameters` on all six operations.
- **H** — both weak descriptions rewritten.

**A published error example must differ from its happy path.** The first fix reused
the same operator and message ids, so `get-not-found/curl-request.adoc` came out
byte-identical to `get/curl-request.adoc`: the guide showed one message id returning
200 under one heading and 404 under the next, with nothing to say what changed, while
the path-parameter description called it an id that does not exist. The 404s now use
a `MISSING_MSG` id, and the 403 uses a **STAFF token** — because that error turns on
who is asking, not on which message, so the URL is necessarily the same one.

The error field list is a shared `ERROR_FIELDS` constant carrying the
`.type(JsonFieldType.STRING).optional()` that an absent `code` needs. Copy it. Its
description says `code` is absent *when the throw site supplied none*, not "as here" —
`InvalidFieldException` and `ConflictException` both carry one, so the first pass
documenting a 422 or 409 would otherwise publish a field table contradicting its own
example.

---

# `audit` — 2026-08-16

## Baseline

Built from scratch, because stale snippets are how this audit lies to itself:

```
./mvnw -o clean package
```

BUILD SUCCESS. **154 snippet directories, 154 `operation::` macros, and every macro
resolves** — the state this pass found, before it changed anything. Counted after
`rm -rf target`, in a scratch copy outside the repo. For the current numbers, run the
commands in the playbook; they have moved with every pass since.

---

## Endpoint table

| method | path | has test | has snippet | in the guide | strict or relaxed | status |
|---|---|---|---|---|---|---|
| GET | `/api/tour-operators/{tourOperatorId}/audit-log` | yes | `audit-log/list` | yes | strict | **documented** |
| GET | `/api/tour-operators/{tourOperatorId}/audit-log/{entryId}` | yes | `audit-log/get` | yes | strict | **no field table** |

Both paths resolved by hand from the class-level `@RequestMapping` plus the method
annotation. There is no context path or servlet path to add.

The context has one controller and two mappings. It is read-only by design: entries
are written by the mutating use cases through `AuditTrailPort`, never through this
API.

---

## A. Undocumented endpoints — none

Both endpoints have a documenting test, a snippet directory and an `operation::`
line.

- **Verified by**: `grep -rln "audit-log\|AuditLogController" src/test/java` →
  `audit/presentation/controller/AuditLogControllerDocumentationTest.java` and the
  `ApiGuideDocumentsEveryListFieldTest` guard. `ls
  target/generated-snippets/audit-log/` → `get`, `list`. `grep -n
  "operation::audit-log" src/docs/asciidoc/api-guide.adoc` → lines 1395 and 1402.

## B. Orphaned snippets — none

Every snippet directory in the repo is pulled in by a macro, and every macro has a
directory behind it.

- **Verified by**: the 154 snippet leaf directories diffed against the 154 unique
  `operation::` names. Zero on either side.

## C. Broken macros — none

No `operation::` names a directory that does not exist after a clean build, so the
"does the build fail or render silently" question did not arise here. It stays
unanswered rather than guessed.

- **Verified by**: `while read o; do [ -d "target/generated-snippets/$o" ] || echo
  MISSING; done` over every macro name. No output.

## D. Stale artifacts — none

No snippet or guide section names an endpoint that no longer exists. `target/` is
gitignored, so no snippets are committed.

## E. Relaxed documentation — none, and not just in this context

**There is no `relaxed*` documentation anywhere in this repository.** The single grep
hit is the English word "relaxed" inside a Javadoc comment.

- **Verified by**: `grep -rn "relaxed" src/test/java` → one line,
  `audit/domain/entity/AuditLogEntryColumnWidthsTest.java:25`, reading "a relaxed
  constant drifts silently". Not a REST Docs call.

This means the strict field check is live on every documented response in the
application. A field added to a documented response fails the build.

## F. Partial coverage — three findings

**F1. `Get an Audit Entry` documents no response fields.** `document("audit-log/get")`
passes only `requestHeaders`. `AuditLogEntryResponse` has **twelve** components, and
none of them reach the guide. The rendered section has no table — the reader gets a
raw JSON body and nothing telling them what `actorType`, `details` or `changes` mean.
The list endpoint, which returns the same record, documents all fifteen paths.

- **Files**: `AuditLogControllerDocumentationTest.java:134`,
  `AuditLogEntryResponse.java`
- **Severity**: medium. A consumer integrating against the single-entry read has to
  guess the shape or go and read the list section.
- **Verified by**: `ls target/generated-snippets/audit-log/get/` → no
  `response-fields.adoc`, while `audit-log/list/` has one. Parsing
  `target/generated-docs/api-guide.html` at `resources-audit-get` → no `<table>` in
  the section.

**This one is known.** MAP's Debt entry says 19 body-returning endpoints document no
field table. At audit time it was **20**, because `audit-log/get` was one of them.
F1 fixed that one, so the count is **19** again and MAP is correct as of this commit.
The full list is at the foot of this report.

**F2. The 404 is tested and not documented.** `getUnknownIs404` asserts
`status().isNotFound()` and never calls `document(...)`, so the snippet is never
emitted and the guide never shows the error shape for this endpoint.

- **File**: `AuditLogControllerDocumentationTest.java:139-147`
- **Severity**: low. The guide documents 404 globally, including the tenant-isolation
  meaning, in its status-code table at line 41.
- **Verified by**: read the test; `ls target/generated-snippets/audit-log/` shows only
  `get` and `list`.

**F3. Neither operation documents its path parameters.** Both tests use URI templates
(`get("/api/tour-operators/{id}/audit-log/{entryId}", OP, ENTRY)`), so REST Docs
*could* emit `path-parameters.adoc`. Neither passes `pathParameters(...)`.

- **Severity**: low here — `{tourOperatorId}` and `{entryId}` are self-describing.
  Worth checking in contexts where a path variable is not.
- **Verified by**: `ls target/generated-snippets/audit-log/{get,list}/` — no
  `path-parameters.adoc` in either.

## G. Prose drift — none, and the section is unusually current

The guide's audit-log prose is accurate against the code, including a change that
landed yesterday: it states that the negating operators answer **422** on `actorId`
and `actorName`, and names them (`not_in` on `actorId`, `neq` and `not_contains` on
`actorName`). That matches `ListAuditLogUseCase.SCHEMA`'s two `.nullable(...)`
declarations exactly.

One inconsistency, not a defect: five other endpoint sections restate "Non-member →
404" and these two do not. The status-code table covers it globally.

- **Verified by**: `src/docs/asciidoc/api-guide.adoc:1383-1401` read against
  `ListAuditLogUseCase.SCHEMA`.

## H. Description quality — none

All fifteen descriptions on the list carry information the field name does not.
Examples: `id` is described as "The entry id (UUIDv7 — descending id IS the
timeline)", and `actorName` as "the acting user's display name, FROZEN at write time;
null for SYSTEM".

---

## UNVERIFIED — needs human check

- **What the build does with a broken `operation::`.** Nothing is broken today, so I
  did not observe it. Introducing one to find out would be a mutation test, and this
  pass changes nothing. Left open deliberately.

---

## Fix these first

Ordered by how likely a consumer is to be misled.

1. **F1 — document the twelve fields on `audit-log/get`.** It is the only finding a
   reader hits directly, and the fix is copying the list endpoint's `responseFields`
   with the `data[].` prefix dropped. Twenty minutes.
2. **F2 — call `document("audit-log/get-not-found")` in the existing 404 test.** The
   assertion is already there; only the snippet and one `operation::` line are
   missing.
3. **F3 — add `pathParameters(...)` to both.** Cosmetic here. Decide it as a
   convention now, because the contexts with 40 and 45 mappings are coming.

## The 19 body-returning operations with no field table

Repo-wide, for the passes that follow. Measured **after** F1, which removed
`audit-log/get` from this list. Matches MAP's Debt entry.

`audiences/get` · `audience-translations/get` · `audience-translations/list` ·
`experience-metafield-translations/get` ·
`experience-metafield-translations/list-locales` · `metafield-definitions/get` ·
`metaobject-field-translations/get` · `metaobject-field-translations/list-locales` ·
`page-metafield-translations/get` · `page-metafield-translations/list-locales` ·
`page-translations/get` · `pickup-locations/get` · `pickup-locations/list` ·
`slots/cancel` · `slots/get` · `slots/list` · `slots/update` ·
`tour-operator-metafield-translations/get` ·
`tour-operator-metafield-translations/list-locales`

**Verified by**: for every `response-body.adoc`, stripping the `----` fences and
checking the body is non-empty, then testing for a sibling `response-fields.adoc`. Two
corrections on the way to this number: a first pass returned 71 by counting 204s with
empty bodies, and the 20 reported mid-audit included `audit-log/get`, which this same
PR fixed.

---

## What was fixed

All three, in the same pass, suite **1227 green** and verified in the rendered HTML
rather than only in the snippets.

- **F1** — `audit-log/get` documents all twelve fields. Descriptions match the list's
  word for word, since it is the same record one level up.
- **F2** — the 404 is documented as `audit-log/get-not-found`, with a guide section
  saying that a missing entry and another operator's entry answer identically.
  **It is not the non-member 404**, and an earlier revision of this report claimed it
  was. A non-member is stopped by `TourOperatorMembershipInterceptor` and gets
  `"Tour operator not found"`; this snippet comes from the use case and says
  `"Audit log entry not found"`. `message` is a documented field, so the two are
  distinguishable. Caught in review.
- **F3** — `pathParameters` on all three operations.

**Two things the fix taught, both worth carrying into the next pass.**

**The error record is `status`, `error`, `message`, `code`, `timestamp` — there is no
`path` field.** I wrote `errorCode` and `path` from memory, then read
`ApiErrorResponse` and corrected both. This is the exact failure the playbook's
"read the actual DTO" rule exists to stop, and it took reading one file.

**`.optional()` is not enough for a field that is absent from the payload.** `code` is
`@JsonInclude(NON_NULL)` and `ResourceNotFoundException` supplies none, so REST Docs
failed with *"Cannot determine the type of the field 'code' as it is not present in
the payload"*. It needs `.type(JsonFieldType.STRING)` as well. Every error response
documented from here on will hit this.

**`audit-log/get-not-found` is the first documented non-2xx in the guide, and it makes
operations outnumber endpoints** — permanently, and by more with every pass that
documents an error. Do not equate the two counts; measure each with the commands in
the playbook. Before this, every operation in the guide documented only its happy
path, while roughly 60 error assertions sat in the suite that no reader could see.
Spreading it further is a decision rather than a mechanical follow-up: cheap per
endpoint, and there are a great many endpoints.

---

*This report is tracked, and one rolling copy covers the whole series: each context's
pass replaces it and adds itself to the header. **It is deleted when the last context
is done**, not after each pass. Anything that outlives the series belongs in
`../MAP.md`.*
