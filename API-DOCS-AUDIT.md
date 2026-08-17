# API-docs sync audit — the rolling report

**Contexts done: `audit`, `contact`, `reference`, `pickup`, `audience`, `media`
(2026-08-16), `page`, `identity` (2026-08-17).** Four to go; `storefront` is last.
Playbook: `API-DOCS-SYNC.md`.

**One section per context, newest first.** Within each, findings A–H are what the
audit found *before* anything changed — the audit pass itself changes nothing, which
is the playbook's rule — and "What was fixed" says what is true now. Read the two
together.

**Every finding raised so far is fixed.**

These results hold repo-wide and save every later pass the work:

- **No `relaxed*` documentation exists anywhere.** That is the whole of finding E,
  and it is closed for every context.
  **It does not mean every response is checked.** An operation that passes no
  `responseFields(...)` at all has no strict check either, and a number of them
  publish a body with no field table — the heading at the foot of this report has the
  current count and the names. **Check field-table coverage in your context; only the
  `relaxed*` question is settled.**
- **Every `operation::` macro resolves and every snippet directory is pulled in**, so
  B, C and D are clean unless a pass breaks them.
- **Every operation publishes a curl and an httpie example.** 18 test classes had
  called `.snippets().withDefaults(httpRequest(), httpResponse())`, which *replaces*
  the default snippet set rather than adding to it, so 61 operations emitted four
  snippets instead of eight and published no copy-pasteable example. All 18 are fixed;
  the call should not reappear. **No guard catches this** —
  `ApiGuideDocumentsEveryEndpointTest` checks that an operation is referenced, not
  what it renders, so fewer snippets is a green build and a thinner page.
- **Any field your fixture leaves null needs an explicit `.type(...)`.** REST Docs
  infers a field's type from the value it sees, so a stubbed null publishes the type
  **`Null`** — telling a client the field can never hold anything. It is not about
  cursors: `nextCursor` was 10 of 14 list endpoints (#172), `page-translations/get`
  published `seoTitle` and `seoDescription` that way while its own list got them right
  (#175), and **six more are still `Null` on `main`**, all in `touroperator` —
  `brand/get`'s three media ids, `tour-operators/get`'s `address.address2`, and
  `acceptedAt` on both invitation reads. Check the whole record, not the cursor.
- **`.optional()` publishes nothing, so a PATCH's partial rule must be in the
  description text.** The default request-fields template renders
  `Path | Type | Description` and has no Optional column, so a create table and its
  PATCH table come out **byte-identical** and the PATCH's fields read as mandatory.
  Write "Omit to keep the current value" — and check what omission actually does:
  `audiences/update` skips null fields, so omitting `paxPerUnit` keeps the current
  value rather than resetting it to the default the create description names.
- **A `{locale}` path variable does not mean the locale is validated.** Only the six
  `Upsert*` use cases consult `OperatorLocalesQuery`. A read or a delete takes the
  same variable, checks its *shape* through `LocaleCode` and nothing else — so a
  locale the operator does not publish is a `200` with nulls, or an idempotent `204`,
  never a 422. Reserve the 422 wording for the upsert.
- **A published error example must differ from the success it contrasts with**, and
  `PublishedExamplesAreHonestTest` fails the build when it does not. Vary the thing
  the error turns on: a missing id for a 404, a **STAFF token** for a 403 (that error
  is about who asks, so the URL has to stay the same), the clashing value for a 409.
  It found **10 instances across five contexts** on its first run, including two this
  series had already fixed by hand in one context and then reproduced in four others.
  It finds an error **by its status line, not by its name** — an earlier version
  matched a hand-kept list of name fragments, which is the same restatement it exists
  to remove, and would have missed the next section called `-gone` or `-too-many`.
- **Do not restate a constant from `src/main` in a description or a stubbed error.**
  A doc test that hand-copies an allowlist or a message keeps publishing the old one
  after the source changes, and the suite stays green because the test stubs the very
  code it copied from. Build the sentence from the source
  (`ContentType.ALLOWED`, `UploadMediaUseCase.MAX_BYTES`) or raise the real exception.
  **Generating the table is only half of it** — the guide's hand-written prose
  describes the same facts, and that half needs its own guard:
  `ApiGuideNamesTheRealAllowlistTest` pins the upload prose against the allowlist both
  ways, because a type the code allows and the guide omits is an undocumented
  capability, and one the guide names and the code refuses is a promise the API
  breaks.
- **The error shape is `status`, `error`, `message`, `code`, `timestamp`.** There is
  no `path` field, and it is `code`, not `errorCode`. `code` is
  `@JsonInclude(NON_NULL)`, so where the throw site supplies none it needs
  `.type(JsonFieldType.STRING)` as well as `.optional()`, or REST Docs cannot infer a
  type and fails the build. **Use `ApiErrorSnippets.errorFields()`** — one copy, in
  `src/test/java/com/vointika/shared/web/docs/`. It covers the 401 too: the entry
  point writes that body by hand, but with a null `code` dropped it is the same four
  keys as the handler's, and `UnauthorizedException` has no code-carrying constructor,
  so no 401 can differ.

---

# `identity` — 2026-08-17

Fourteen endpoints. **A–E clean**, and no path variables anywhere, so that gap is
correctly absent rather than missed.

**This is the only context with public unauthenticated routes, and the only one whose
documentation test asserted no non-2xx at all** — on the richest error surface in the
application: three distinct 401s on refresh, a 401 on login, a 401 on change-password,
and four 422s on the avatar.

## F1. A rejected refresh has three causes and one of them ends every session

`RefreshAccessTokenUseCase` answers **the same 401 with the same message** for an
unknown token, an expired one, and a **replayed** one. That sameness is deliberate —
telling a caller they tripped the reuse detector tells an attacker the same thing.

The consequence is not the same. A replayed token is treated as a theft signal and
`revokeAllByFamilyId` ends **every session descended from that login**. A client that
retries a stale refresh, or races two tabs through a rotation, is logged out
everywhere with nothing in the response explaining why.

- **Severity**: high, and the most consequential undocumented behaviour found so far.
  It is not deducible: the response is byte-identical to the benign case.
- **Verified by**: `RefreshAccessTokenUseCase:58-63` for the reuse branch, `:65-66`
  for expiry, `:93-94` for the rotation race — which is deliberately *not*
  reuse-detection, and says so.

## F2. The avatar restated its allowlist and its cap

`"image/jpeg, image/png or image/webp, max 5 MB"` was hand-written in the part
description, and `SetAvatarUseCase` hardcoded `"File too large: max 5 MB"` beside
`MAX_AVATAR_BYTES`. **This is exactly the media defect from #174, in a second
context** — including the production half: raising the cap would refuse a 6 MB file
while telling the caller the limit is 5.

## F3. Refresh and Set Avatar had no prose at all

Two of the three most consequential endpoints in the API carried a heading, a macro
and nothing else.

## G. Prose drift — none

## H. Description quality — none

## What was fixed

Three new tests, each publishing an error nothing showed:
`aReplayedRefreshTokenIs401AndEndsEverySession`, `badCredentialsAre401`,
`anUnsupportedAvatarTypeIs422`.

- **F1** — `auth/refresh-invalid` publishes the 401, under a guide section stating the
  three causes and the consequence: *treat any 401 here as "start again at login",
  never as "retry"*.
- **F2** — `SetAvatarUseCase` exposes `MAX_AVATAR_BYTES`, `allowedContentTypes()`,
  `tooLargeMessage()` and `unsupportedTypeMessage()`; the throw sites and the published
  description all derive from them. Probed: raising the cap to 8 MB moves the published
  part description with it.
- **F3** — prose for both, plus `auth/login-invalid` documenting that a wrong password
  and an unknown address answer identically.
- The avatar is now inside `ApiGuideNamesTheRealAllowlistTest` rather than left as a
  second chance to drift.

**The lesson repeated across contexts:** media's F1 was not a media bug. The same
hand-copied allowlist and hardcoded cap sat in `identity`, unguarded, while the guard
written for it covered only the media section. A rule recorded for one context does
not reach the next unless something executes it.

---

# `page` — 2026-08-17

Twelve endpoints across two controllers, the largest context so far. **A–E clean.**
The finding is one shape repeated twelve times, plus a conflict a client cannot
diagnose.

## Endpoint table

Eight on `PageController` — list, get, create, update, publish, unpublish, rename,
delete — and four on `PageTranslationController` — list, get, upsert, delete. All
twelve had a test, a snippet and a macro.

## F1. Not one of the twelve documented a path variable

Every endpoint takes `{tourOperatorId}`, ten take `{pageId}`, three take `{locale}`.
None was described. It is the same gap `pickup` and `audience` had, at four times the
size.

## F2. `page-translations/get` published no field table

Six components on `PageTranslationResponse`, none documented — while the sibling list
documented all six.

## F3. The rename has two different 409s and published neither

This is the one worth the pass. `RenamePageUseCase` refuses a handle for **two
distinct reasons** with two distinct messages:

- another page's **canonical** handle — the obvious case; and
- a handle another page uses as a **localized** handle.

The second is PATTERNS §4d reaching a client. A storefront address resolves against
localized handles first and canonical ones second, so the two are one namespace: taking
a value from either would make the other page unreachable in that language. **The
operator sees no page called that in their own language**, so without the distinct
message and its own example, "already exists" reads as a bug.

Neither was tested. `createDuplicateHandleIs409` covered the plain case on create only.

## F4. `pages/get`'s 404 was tested and not published

## G. Prose drift — two, and this pass first reported none

**`PATCH /pages/{pageId}` is not partial**, and nothing published said so.
`UpdatePageUseCase` builds `new PageTitle(input.title())` and
`new PageBody(input.body())` **unconditionally**, and both reject null — so sending
only the field you changed is a 422. The field descriptions said "(whole replace)",
which describes what happens to the value, not that the field is required on every
call. And this series had taught the opposite convention two contexts earlier:
`pickup-locations/update` is a PATCH whose fields say "Omit to keep the current
value". Carrying that across lands on a 422.

**The rename section had the breakage backwards.** It said allowing the rename would
make *the other page* unreachable. `StorefrontPageQueryImpl.findByHandle` resolves the
localized handle first, so that address keeps matching the page that holds it — and
the **renamed** page is the one left with no address in that language. The refusal
protects the renamer. Getting it inverted undercut the section, since its whole point
is that the operator cannot see the conflict.

## H. Description quality — none

## What was fixed

**Four new tests**, all publishing an error this context raised and never showed:
`renameOntoALocalizedHandleIs409`, `updateWithoutEveryFieldIs422`,
`unsupportedLocaleIs422` and `aLocalizedHandleTakenByAnotherPagesCanonicalIs409`.
Three of the four came out of review rather than the audit.

*(No suite total here. The line originally read "1236 → 1237, one new test" and was
stale within the hour, because a review round adds work after the summary is written —
which is how every pinned count in this series went stale. Names do not.)*

- **F1** — `pathParameters` on all twelve.
- **F2** — the field table, which takes the tracked list **17 → 16**.
- **F3** — `pages/rename-conflict` publishes the cross-namespace refusal, under a
  guide heading that explains why a handle the operator cannot see is taken.
- **F4** — `pages/get-not-found` and `pages/create-conflict` publish.

**`page-translations/upsert` publishes zero errors while raising five**, and two of
them cannot be deduced: the 422 for a locale the operator has not enabled, and a 409
for a localized handle equal to **another page's canonical** handle. That second one
is F3 pointing the other way — and it is the direction an operator exercises, since
localized handles get set routinely and renames are rare. Both are published now.

**Both new guards fired on this pass, which is the first time they have run against
work they did not already cover.** `ApiGuideDocumentsEveryEndpointTest` caught three
operations with no `operation::` line. `PublishedExamplesAreHonestTest` caught
`pages/get-not-found` and `pages/create-conflict` publishing the same request as their
happy paths — the defect that took two review rounds to find by hand in `contact`, now
caught before the PR opened.

---

# `media` — 2026-08-16

Five endpoints, and **the best-documented context in the series so far** — every
operation already had its headers, and four of the five had their field tables. The
findings are about what the documentation *said*, not what it omitted.

## Endpoint table

| method | path | has test | has snippet | in the guide | strict or relaxed | status |
|---|---|---|---|---|---|---|
| POST | `…/media` | yes | `media/upload` | yes | strict | **part described wrongly** |
| GET | `…/media` | yes | `media/list` | yes | strict | documented |
| GET | `…/media/{mediaId}` | yes | `media/get` | yes | strict | documented |
| PATCH | `…/media/{mediaId}` | yes | `media/describe` | yes | strict | documented |
| DELETE | `…/media/{mediaId}` | yes | `media/delete` | yes | n/a | 204, no body |

**A–E: none.** This is also the first context using `requestParts` and
`responseHeaders`, both already correct.

## F1. The upload part advertised a wider allowlist than the code accepts

The published part description read **"image/* or application/pdf, ≤ 25 MB"**. The
allowlist is exactly four types — `image/jpeg`, `image/png`, `image/webp`,
`application/pdf`.

**`image/gif` and `image/svg+xml` both match `image/*` and are both refused**, and
SVG is refused *deliberately*: an SVG can carry script and these files are served
from a public bucket. So the one type a reader most needs warning about was the one
the description implied was fine.

- **Severity**: medium, with a security edge. A client following the guide uploads an
  SVG, gets a 422, and has no way to know the refusal is intentional rather than a
  bug.
- **Verified by**: `ContentType.java:21-24` for the map, `:38` for the message, and
  the generated `media/upload/request-parts.adoc` for what was published.
- **The guide's own prose was right** — it lists all four types two lines above the
  table that contradicted it. Prose and contract disagreed, and only the contract is
  machine-readable.

## F2. Three 422s define the endpoint and none was documented

The allowlist, the 25 MB cap, and a zero-byte part — checked in that order, before
the cap.

**They were already tested.** `UploadMediaUseCaseTest.rejectsDisallowedContentType`
and `rejectsEmptyAndOversizeFiles` cover all three on `main`. An earlier revision of
this section said "neither tested nor documented", which was a behavioural claim
reached by reading rather than running — the one thing LAW §4 names outright. Caught
in review.

What was missing was the **published** contract, which is this series' remit. The new
tests add no behavioural coverage: they stub the use case and assert only that
`GlobalExceptionHandler` maps `InvalidFieldException` to 422. They do not need to,
because the behaviour was covered.

## F3. Three error assertions existed and published nothing

401, 403 and the tenant 404.

## F4. No path parameters on upload or list

## G. Prose drift — see F1

## H. Description quality — none

## What was fixed

Suite **1231 → 1233**. Two new tests, both for the 422s.

- **F1** — the part description is **generated from `ContentType.ALLOWED`** and the
  cap from `UploadMediaUseCase.MAX_BYTES`, so adding a type updates the guide by
  itself. The first fix hand-copied the four types, which re-created the drift class
  F1 exists to report — the suite would have stayed green with the guide advertising
  the old set, because the test stubs the code it copied from. The 422 example now
  raises the refusal from the real `ContentType`, and fails loudly if SVG is ever
  allowed.
- **F2** — `media/upload-unsupported-type` (an actual SVG), `media/upload-too-large`
  and `media/upload-empty` publish all three. The audit itself missed the empty-file
  case twice before review found it.
- **F3** — `media/upload-forbidden` and `media/list-not-found`. The 401 stays central.
- **F4** — path parameters on both.

**The lesson is that a documented context is not a correct one.** Every previous pass
found things missing; this one found a field table that was complete, published, and
wrong in the direction a reader would act on. The categories that scan for absence —
A through E — were clean here.

---

# `audience` — 2026-08-16

Eight endpoints across two controllers. **A–E clean.** The two were in very different
states: `AudienceController` was largely documented, `AudienceTranslationController`
documented nothing at all.

Eight error assertions already sat in the two tests. **None was published**, and two
are shapes this series had not met before.

## Endpoint table

| method | path | has test | has snippet | in the guide | strict or relaxed | status |
|---|---|---|---|---|---|---|
| GET | `…/audiences` | yes | `audiences/list` | yes | strict | documented |
| GET | `…/audiences/{audienceId}` | yes | `audiences/get` | yes | **no fields** | body undocumented |
| POST | `…/audiences` | yes | `audiences/create` | yes | strict | documented |
| PATCH | `…/audiences/{audienceId}` | yes | `audiences/update` | yes | strict | documented |
| GET | `…/audiences/{audienceId}/translations` | yes | `audience-translations/list` | yes | **nothing** | undocumented |
| GET | `…/translations/{locale}` | yes | `audience-translations/get` | yes | **nothing** | undocumented |
| PUT | `…/translations/{locale}` | yes | `audience-translations/upsert` | yes | **nothing** | undocumented |
| DELETE | `…/translations/{locale}` | yes | `audience-translations/delete` | yes | **nothing** | undocumented |

## F1. The translation controller documented nothing

All four `document(...)` calls passed the operation name and no snippets — the shape
`pickup` had. Three of the four return or accept a body and published no field table,
and none documented the `{locale}` path variable, which is the one a caller is most
likely to get wrong.

## F2. `audiences/get` published no field table

The list documented all five components of `AudienceResponse`; the single read
documented none.

## F3. Eight error assertions, none published

Two are new to the series:

- **422 on an unsupported locale.** The locale must be one the operator publishes in,
  so a valid ISO code the operator has not enabled is still a 422 — a distinction no
  reader could have drawn from the guide.
- **400 on a missing body, raised by Spring MVC rather than by the application.**
  `@RequestBody` is required by default, so the request is rejected before the handler
  runs; the test records that this is why the handler's own null guard was deleted as
  unreachable. **The response is still the standard error shape**, because
  `GlobalExceptionHandler` extends `ResponseEntityExceptionHandler` and maps it
  through `handleExceptionInternal`.

The rest were 401, 403 twice, 409 and 404 — all published now except the 401, which
stays central.

## G. Prose drift — one, and this pass first reported none

`audiences/update` published a request table **byte-identical to create's**, so a
caller reading it expects `paxPerUnit` to reset to 1 when omitted. `UpdateAudienceUseCase`
skips null fields, so it keeps the current value. Pre-existing on `main`, not
introduced here — but the trap and its fix were established one commit earlier in this
same series, on `pickup`, and this pass reported the category clean. The rule was
sitting in `pickup`'s findings instead of in the repo-wide block where every pass
would read it; it is in the block now.

## H. Description quality — none

## What was fixed

Suite unchanged at **1231**. No new tests; eight existing assertions taught to publish.

- **F1** — all four translation operations document headers, path variables and their
  bodies. The upsert states that a blank name **clears** the overlay rather than
  storing an empty one.
- **F2** — `audiences/get` gains its field table. With the three translation reads,
  the tracked no-field-table list drops **20 → 17**.
- **F3** — six new operations: 403, 409 and 404 on audiences; 403, 422 and 400 on
  translations. Each has a guide section saying when it happens.
- Path parameters added where they were missing, on `audiences/list` and
  `audiences/create`.
- **The `{locale}` description promised a 422 that two of the four operations cannot
  return.** One constant was reused across all four. Only the upsert consults
  `OperatorLocalesQuery`; `GET` falls back to an empty translation (**200**) and
  `DELETE` returns early (**204**), so a client branching on 422 to detect "locale not
  enabled" would never see one. The reads say "BCP-47 locale code" now, matching the
  precedent in `ExperienceTranslationControllerDocumentationTest`, and the guide says
  outright that an unpublished locale is indistinguishable from an untranslated one.
  Caught in review.
- **`audiences/update` states its partial semantics** per field, per G above.

**The 400 is worth carrying forward.** It is the first documented error the
application does not raise — Spring MVC does, before any of our code runs. It still
answers the standard shape, so `ApiErrorSnippets.errorFields()` covers it, and the
guide section says where it comes from. Every context with a required request body has
this same 400, and none of them document it.

---

# `pickup` — 2026-08-16

Five endpoints, all five referenced by the guide — and **all five documented
nothing**. Every `document(...)` call passed the operation name and no snippets at
all, so the guide published a curl and a raw body for each and not one field table,
path variable or header. Four error assertions already sat in the test and none was
published.

This is the largest gap in the series so far, and the first context where the
*contract* was missing rather than the examples.

## Endpoint table

| method | path | has test | has snippet | in the guide | strict or relaxed | status |
|---|---|---|---|---|---|---|
| GET | `…/pickup-locations` | yes | `pickup-locations/list` | yes | **no fields** | body undocumented |
| GET | `…/pickup-locations/{pickupLocationId}` | yes | `pickup-locations/get` | yes | **no fields** | body undocumented |
| POST | `…/pickup-locations` | yes | `pickup-locations/create` | yes | **no fields** | **request** undocumented |
| PATCH | `…/pickup-locations/{pickupLocationId}` | yes | `pickup-locations/update` | yes | **no fields** | **request** undocumented |
| DELETE | `…/pickup-locations/{pickupLocationId}` | yes | `pickup-locations/delete` | yes | n/a | 204, no body |

**A–E: none.** Five mappings, five tests, five snippet directories, five macros.

## F1. Two request bodies were entirely undocumented

`POST` and `PATCH` both take a `PickupLocationInput`, and neither published a
`request-fields.adoc`. A client had the raw JSON example and nothing else: no field
list, no statement that `name` is unique per operator, and **no statement that PATCH
is partial** — which is the difference between updating a time and clearing a name.

- **Severity**: high. This is the only finding in the series so far where a reader
  cannot construct a correct request from the guide.
- **Verified by**: `ls target/generated-snippets/pickup-locations/create` → no
  `request-fields.adoc`; `PickupLocationInput` read in full.

## F2. Both reads published no field table

`list` and `get` return `PickupLocationResponse` and documented none of its five
components. Both are on the tracked no-field-table list; fixing them takes it from 22
to 20.

## F3. Four error assertions existed and published nothing

The test already asserted 401, 403, 409 and 404. Every one passed, and no reader
could see any of them.

- **403** — a STAFF member may list and read but not create. The membership check has
  already passed, so it is 403 rather than the 404 a non-member gets.
- **409** — the name is unique per operator, compared **case-insensitively**, so
  `Old Port` and `old port` collide. This is the API's first documented 409.
- **404** — a non-member on any tenant-scoped route.
- **401** — already covered by the canonical `authentication/unauthorized`.

## F4. No path parameters on any of the five

## G. Prose drift — none, but two rules lived only in prose

"The name is unique per operator, case-insensitively → 409" and "Partial" were
written in the guide and nowhere a client could see them in machine-readable form.
Both are now field descriptions as well.

## H. Description quality — none (there were no descriptions)

## What was fixed

Suite unchanged at **1231** — no new tests, four existing ones taught to publish what
they already asserted, which is the cheapest kind of documentation there is.

- **F1** — `requestFields` on create and update, with the partial-update rule in the
  **description text** of each field. `.optional()` alone publishes nothing: the
  default request-fields template renders `Path | Type | Description` and has no
  Optional column, so the create and update tables came out identical and update's
  fields read as mandatory. Caught in review, after an earlier revision of this
  section claimed the rule was "stated per field" when it was still only in prose.
- **F2** — `responseFields` on both reads. The tracked list drops **22 → 20**.
- **F3** — `create-forbidden`, `create-conflict` and `list-not-found` publish the
  403, 409 and 404, each with its own guide section explaining when it happens. The
  401 was deliberately **not** duplicated here: it is one filter's answer for the
  whole API and is documented once under Authentication.
- **F4** — `pathParameters` on all five.
- **Repo-wide, found here** — every `nextCursor` descriptor is explicitly typed. See
  the header block; 10 of 14 lists were publishing type `Null`, including
  `audit-log/list` and `contact-messages/list`, which this series documented and
  missed twice.

**The rule this context settles: an error belongs where the rule that produces it
lives.** The 404 for a non-member is the interceptor's and applies to every
tenant-scoped route, but it is documented here rather than centrally because it is
the first place a reader meets it — and the section says so. The 401 is the filter
chain's and is documented once. The 403 and 409 are this endpoint's own.

---

# `reference` — 2026-08-16

Four endpoints, all four documented, **A through E clean**. Two findings, and the
first one is the kind only a rendered-output check catches.

## Endpoint table

| method | path | has test | has snippet | in the guide | strict or relaxed | status |
|---|---|---|---|---|---|---|
| GET | `/api/currencies` | yes | `currencies/list` | yes | strict | **no curl example** |
| GET | `/api/countries` | yes | `countries/list` | yes | strict | documented |
| GET | `/api/timezones` | yes | `timezones/list` | yes | strict | **no curl example** |
| GET | `/api/languages` | yes | `languages/list` | yes | strict | **no curl example** |

The first context with **no tenant scoping**: no path variables, no membership
interceptor, no roles. Authenticated and nothing more, so the only error any of them
can return is 401.

## F1. Three of the four published no copy-pasteable example

`CurrencyControllerDocumentationTest`, `TimezoneControllerDocumentationTest` and
`LanguageControllerDocumentationTest` each called

```java
.snippets().withDefaults(httpRequest(), httpResponse())
```

`withDefaults` **replaces** the default snippet set rather than adding to it, so those
three emitted four snippets where every other operation emits eight — losing
`curl-request`, `httpie-request`, `request-body` and `response-body`.
`CountryControllerDocumentationTest` never did it and got the full set.

The result was visible in the published guide: **List Countries showed a `curl`
command and the three endpoints beside it, in the same section, showed none.**

**It was never a `reference` problem.** An earlier revision of this section said
"every other operation emits eight", generalising from an `ls` of two directories —
one of which happened to be the class that never restricted anything. Measured across
every snippet directory: **18 classes and 61 operations**, including all ten
`touroperator` classes, with `AuthControllerDocumentationTest` alone accounting for
13. Only 98 of 159 operations published a curl example. All 18 are fixed here, so the
count is now **zero**.

- **Severity**: medium. Nothing is wrong or missing in the *contract* — the field
  tables are complete. What is missing is the thing a reader copies.
- **Verified by**: `ls target/generated-snippets/{countries,currencies}/list` → 8
  files against 4. Then parsing `target/generated-docs/api-guide.html` per section:
  `$ curl` present under `resources-countries`, absent under the other three.
- **Why no guard caught it**: `ApiGuideDocumentsEveryEndpointTest` checks that an
  operation is *referenced*, not what it renders. A `document(...)` call that emits
  fewer snippets is a green build and a thinner page.

## F2. The 401 was documented nowhere, and its shape is not the shared one

Every endpoint in this context is authenticated, so 401 is the only error they have,
and no operation in the guide published it.

**It is the ordinary error body**, and an earlier revision of this section said it was
not. `RestAuthenticationEntryPoint` writes the JSON by hand, but
`GlobalExceptionHandler:62` serializes `ApiErrorResponse` with a null `code` that
`@JsonInclude(NON_NULL)` drops — leaving the same four keys in the same order. The two
are indistinguishable to a client. **No 401 in this API can carry a `code` either
way**: `UnauthorizedException` declares only a message constructor and the handler
calls the two-argument `build`.

- **Verified by**: `RestAuthenticationEntryPoint:24-30`, `GlobalExceptionHandler:62`
  and `UnauthorizedException` read in full. Caught in review, in three places at once
  — this section, the guide prose and the test javadoc — where the last of the three
  directly contradicted `RestAuthenticationEntryPoint`'s own javadoc.

## G. Prose drift — none, but the section is uneven

Countries, timezones and languages carry explanatory prose; **currencies has none at
all**. Languages says "Authenticated." where the other three do not, though all four
are. Not wrong, just inconsistent — left alone rather than widened into.

## H. Description quality — none

## What was fixed

- **F1** — the three tests stopped overriding the snippet defaults. All four
  endpoints now emit eight snippets and render a curl and an httpie example, matching
  the rest of the guide.
- **F1, repo-wide** — the `withDefaults` call is gone from all 18 classes, not just
  the three this context owns. All 159 operations now publish a curl and an httpie
  example; the count without one is zero.
- **F2** — the 401 is documented **once, for the whole API**, as
  `authentication/unauthorized` under a new *Without a Token* heading in the
  Authentication section. It is published from the currencies test because that is
  the simplest surface — no path variables, no tenant, no roles — but the refusal
  happens in the filter chain before any controller, so it is every endpoint's 401.
  It documents itself with the shared descriptor, which makes it the worked example
  the remaining passes copy.
- **The error descriptor moved to one place**: `ApiErrorSnippets.errorFields()` in
  `shared/web/docs`. It was a private constant inside the contact test that two
  contexts had already begun reaching for.

**A per-endpoint error and an API-wide one are documented differently.** `contact`'s
403 belongs to its endpoint because the rule that produces it is that endpoint's.
This 401 belongs to the Authentication section because one filter produces it for
everything. Ask which before adding the next one.

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

**This one is known.** MAP's Debt entry said 19 body-returning endpoints document no
field table at the time of this pass. This pass measured **20**, because
`audit-log/get` was one of them, and F1 took it off the list. For the current number
and the full list, see the heading at the foot of this report — it has moved twice
since and should be read there rather than here.

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

## The 16 body-returning operations with no field table

Repo-wide, for the passes that follow. **MAP's Debt entry points here rather than
pinning a number**, which is why this heading is the only place it is stated.

**The old scan was blind to exactly the set F1 found.** Its method is: for every
`response-body.adoc`, strip the fences, check the body is non-empty, then test for a
sibling `response-fields.adoc`. The 61 operations restricted by `withDefaults` emitted
no `response-body.adoc` at all, so the scan never saw them. Removing that call
repo-wide made three more visible: `experiences/get`, `experiences/translations/get`
and `experiences/translations/list`. The two defects were one defect.

`experience-metafield-translations/get` ·
`experience-metafield-translations/list-locales` · `experiences/get` ·
`experiences/translations/get` · `experiences/translations/list` ·
`metafield-definitions/get` · `metaobject-field-translations/get` ·
`metaobject-field-translations/list-locales` ·
`page-metafield-translations/get` · `page-metafield-translations/list-locales`
· `slots/cancel` · `slots/get` · `slots/list` · `slots/update` ·
`tour-operator-metafield-translations/get` ·
`tour-operator-metafield-translations/list-locales`

**Verified by**: for every `response-body.adoc`, stripping the `----` fences and
checking the body is non-empty, then testing for a sibling `response-fields.adoc` —
re-run after the repo-wide `withDefaults` removal, without which it cannot see 61 of
the 159 operations. The number moves as passes fix their own: it has read 71 (a bad scan), 20, 19 (blind
to the restricted set), 22 (the corrected scan), and 20 again since `pickup`
documented its two reads. Re-run the scan rather than trusting any of them.

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
