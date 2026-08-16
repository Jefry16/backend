# API-docs sync audit — `audit` — 2026-08-16

First pass of the per-context series. Investigation only; no code or docs changed.

Playbook: `API-DOCS-SYNC.md`. Order: `audit` first, `storefront` last.

**All three findings are fixed** (see "What was fixed" at the foot). The audit below
is the state it found, kept so the next pass can see what this one looked for.

Two categories came back empty repo-wide, which is the useful result for the eleven
contexts still to come.

---

## Baseline

Built from scratch, because stale snippets are how this audit lies to itself:

```
./mvnw -o clean package
```

BUILD SUCCESS. **154 snippet directories, 154 `operation::` macros, and every macro
resolves.** Both numbers counted after `rm -rf target`, in a scratch copy outside the
repo.

---

## Endpoint table

| method | path | has test | has snippet | in the guide | strict or relaxed | status |
|---|---|---|---|---|---|---|
| GET | `/api/tour-operators/{tourOperatorId}/audit-log` | yes | `audit-log/list` | line 1395 | strict | **documented** |
| GET | `/api/tour-operators/{tourOperatorId}/audit-log/{entryId}` | yes | `audit-log/get` | line 1402 | strict | **no field table** |

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
operations outnumber endpoints.** There are now 155 operations against 154 endpoints,
so any later pass that equates the two counts is off by one. Every one of the other 154
operations documents only its happy path, while roughly 60 error
assertions sit in the suite untested by any reader. Whether to spread this to the
other contexts is a decision, not a mechanical follow-up — it is cheap per endpoint
and it is 154 endpoints.

---

*This report is tracked, and is deleted once its findings ship. Anything durable
belongs in `../MAP.md`.*
