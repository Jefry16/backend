# API-docs sync audit — the rolling report

**Contexts done: `audit`, `contact`, `reference`, `pickup`, `audience`, `media`
(2026-08-16), `page`, `identity`, `experience`, `touroperator`, `metafield`
(2026-08-17).** One to go: `storefront`.
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
- **A request table is only as complete as the fixture that exercises it.** Strict
  `requestFields` fails on an undocumented field **present** in the payload — never on
  a documented field absent from it. So a fixture sending seven of a record's ten
  fields produces a seven-field table and a green build, and a client copying it omits
  whatever was left out. `experiences/create` and `/update` published seven of ten
  that way, one of them (`startingPrice`) required. **Read the request record, not the
  fixture.**
- **Any field your fixture leaves null needs an explicit `.type(...)`.** REST Docs
  infers a field's type from the value it sees, so a stubbed null publishes the type
  **`Null`** — telling a client the field can never hold anything. It is not about
  cursors: `nextCursor` was 10 of 14 list endpoints (#172), `page-translations/get`
  published `seoTitle` and `seoDescription` that way while its own list got them right
  (#175), and `touroperator` carried the last nine — three media ids each on
  `brand/get` and `brand/update`, `tour-operators/get`'s `address.address2`, and
  `acceptedAt` on both invitation reads. **The repo-wide count is now zero.** Keep it
  there with

  ```
  grep -rc '^|`+Null+`$' target/generated-snippets --include='*-fields.adoc' | grep -v ':0'
  ```

  It read six until that scan was widened from `response-fields.adoc` to
  `*-fields.adoc` — **request tables carry this defect too**. Check the whole record,
  not the cursor.
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
- **Walk each published error example back through its use case and ask whether the
  pair can happen at all.** Differing from the happy path is not the same as being
  reachable, and no guard checks reachability — every documentation test mocks the
  use case it documents, so a stubbed message is published whatever request sits
  beside it. `touroperator` published two impossible pairs: `role: OWNER` against
  "You cannot change your own role" (an ADMIN is refused at `ensureOwner` first and
  an OWNER gets the other message), and a `main-menu` create shown as 201 four
  sections above prose saying that handle is seeded for every operator. **Read the
  guards in the order the use case runs them** — that is what decides which message
  a payload actually reaches.
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

  **This has now happened three times — `media`'s 25 MB, `identity`'s avatar cap,
  `touroperator`'s menu depth — so treat it as the default suspicion, not a
  discovery.** Before publishing any limit, grep the number across `src/`: if the
  constant is private and the figure appears more than once, that is the defect.
  There is a second resolution besides deriving, and it is often better for guide
  prose: **reword so the sentence carries no number at all** and let the generated
  table or the published error body state it. The menu cap went that way — the guide
  now says "nesting past the cap" and points at the two places that name it — which
  leaves nothing to guard, rather than one more guard to keep true.
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

# `metafield` — 2026-08-17

Forty-five endpoints across ten controllers — the largest surface in the
application, and the last one before `storefront`. **A–E clean**: 45 mappings, 45
snippet directories, 45 macros.

**It carries the whole of the repo's remaining documentation debt.** All nine
body-returning operations with no field table are here. So are the last three
copies of a hand-written type list, and the guide's only impossible status code.

## Endpoint table

All 45 have a test, a snippet and a macro. What differs:

| controller | endpoints | what is missing |
|---|---|---|
| `MetafieldDefinitionController` | 5 | `ownerType` omits a third of the model; `get` has no field table |
| `MetaobjectDefinitionController` | 8 | every error but one |
| `MetaobjectController` | 7 | every error but one |
| `TourOperatorMetafieldController` | 3 | the definition-first rule |
| `ExperienceMetafieldController` | 3 | same |
| `PageMetafieldController` | 3 | same |
| the four `*-translation` controllers | 16 | no request table on any upsert; no field table on any read |

**Zero of the 45 documents a path variable**, and **zero publishes an error** —
against 34 throw sites and 6 assertions.

## F1. `ownerType` is published as two values and the model has three

The create table reads *"Which resource kind: experience or page (immutable)"*.
`MetafieldOwnerType` is `EXPERIENCE`, `PAGE` and **`TOUR_OPERATOR`** — the
operator's own metafields, Shopify's `shop.metafields`, and the answer to anything
the storefront needs that has no other home.

The guide already documents three endpoints for them
(`tour-operators/metafields/{list,upsert,delete}`). So the guide describes how to
read and write operator metafields while the only table that says which
definitions may exist says that owner type is not one of them. **A client cannot
create the definition those three endpoints need**, and the definition must exist
first — see F7.

- **Severity**: highest here. It is not an omission a reader can route around: the
  value endpoints 404 without a definition.
- **Verified by**: `MetafieldOwnerType:23-25`;
  `MetafieldDefinitionControllerDocumentationTest:126`; the generated
  `metafield-definitions/create/request-fields.adoc`.

## F2. Not one of the 45 documents a path variable

These are the deepest paths in the API — up to four variables:

```
/api/tour-operators/{tourOperatorId}/experiences/{experienceId}/metafields/{namespace}/{key}
```

`{namespace}` and `{key}` **are the metafield's identity**, they are handle-shaped
rather than ids, and their pairing is what a definition is keyed by. Nothing says
so anywhere a reader looks. `{locale}` on the sixteen translation routes is
undocumented too.

## F3. The nine type codes are hand-written in three places, and the sibling enum
in the same package already does it right

`MetafieldType` declares nine constants. The list is then written out again in
`MetafieldType.fromCode`'s refusal (`:56-59`) and a third time in the doc test's
`type` description (`:129`), which is **published**.

The type catalogue is expected to grow — the enum's own javadoc says *"list and
color types are deliberately still out"*. Adding one leaves the refusal naming nine
and the guide naming nine, with a green build.

**`MetafieldOwnerType.fromCode:53` is the fix, already written, four files away:**

```java
+ String.join(", ", Arrays.stream(values()).map(MetafieldOwnerType::code).toList()));
```

Two enums, same package, same job, one derives and one restates.

## F4. The guide restates `isTranslatable()`

*"Only text types can be translated — `single_line_text` and `multi_line_text`"*
(`api-guide.adoc:1668`). That is `MetafieldType.isTranslatable()` copied into
prose, and the predicate's javadoc explicitly anticipates being widened
(*"Both stay out until someone names a case; widening this predicate changes no
schema"*). Widening it leaves the guide wrong and the build green.

This is the fourth appearance of the class the repo-wide block now names.

## F5. No `*-translations/upsert` documents its request body

All four take a `@RequestBody` and pass no `requestFields` at all, so a client gets
a raw JSON blob. The three rules that make the payload usable live only in guide
prose: keys are **`namespace.key`**-qualified, a blank value **clears** that key,
and a key left out is **untouched**. None is machine-readable, and the first is
also a 422 (F7).

## F6. Nine operations publish a body with no field table — the whole tracked list

The four `*-translations/get`, the four `*-translations/list-locales`, and
`metafield-definitions/get`. This is the entire repo-wide list at the foot of this
report; nothing outside `metafield` remains on it.

`metafield-definitions/get` is the familiar shape — its own list documents the
record and the single read documents none.

## F7. Zero errors published, against 34 throw sites

Six assertions exist; none publishes. The rules a client cannot deduce:

- **A value needs its definition first.** `PUT …/metafields/{namespace}/{key}` on an
  undefined pair is **404**, not an implicit create. This is the model's central
  rule and it is a 404 on a PUT, which reads like a routing bug.
- **A `metaobject_reference` value is checked for integrity, not just shape** — the
  entry must exist, be this operator's, and be **of the pinned type** → 422.
- **Two concurrent first-sets** → 409 *"set concurrently — retry"*, which is
  retryable where most 409s here are not.
- **A translation key must be `namespace.key`** → 422.
- **Translating a metafield that has no value** → 404.
- **Translating a non-translatable type** → 422, naming the key (F4).
- **A metaobject definition must keep at least one field** → 409 on remove-field.

## G. Prose drift — one, and it promises a status the code cannot return

**`Delete a Metafield Definition` says *"Cascades to every value. A type still
pinned by a reference definition → 409."*** `DeleteMetafieldDefinitionUseCase` has
**zero** throw sites beyond the 404 for a missing id. It cascades unconditionally.

The sentence belongs to the *metaobject* definition delete, which does refuse a
pinned type — and says so correctly at `api-guide.adoc:1856`. It has been copied
onto the neighbouring endpoint, where it is false.

A client builds "409 means something still references this, warn the operator"
and never sees it. The truth is the opposite and more dangerous: deleting a
metafield definition **silently destroys every value on every resource**, with no
guard at all. The first half of the sentence says so; the second half tells the
reader they are protected.

- **Verified by**: `DeleteMetafieldDefinitionUseCase` read in full — 404 and
  nothing else; `DeleteMetaobjectDefinitionUseCase:56-57` for the real 409.

## H. Description quality — none

Checked mechanically across every `*-fields.adoc` in the context: no description
equals its field name.

## What was fixed

Suite **1253 → 1256**; operations **210 → 219**. Nine new operations, all errors.

**The repo-wide no-field-table list is now empty.** It has been tracked at the
foot of this report since the first pass, reading 71, 20, 19, 22, 20, 16 and 9.
`metafield-definitions/get` was the last one. That heading is deleted.

- **F1** — `ownerType` derives from `MetafieldOwnerType.codes()` and publishes all
  three. It was wrong in **two** published tables, not one: the create request and
  the list response both said "experience or page", and the audit found only the
  first. `MetafieldOwnerType.fromCode` had already been fixed once, with a comment
  saying the old message "still said experience, page and would have kept saying
  it" — and the two published copies were not fixed with it. That comment now lives
  on `codes()` as the reason the helper exists.
- **F2** — `pathParameters` on all 45, generated from the URI template each test
  already used rather than hand-written per operation. `{namespace}`/`{key}` say
  they name an existing definition, which is F7's rule where a reader meets it.
- **F3** — `MetafieldType.codes()`, matching its sibling. The refusal, the create
  table and the get table all build from it.
- **F4** — `MetafieldType.translatableCodes()`, derived from `isTranslatable()`.
  The guide's prose no longer states the set; the published request table does.
- **F5** — a `requestFields` on all four `*-translations/upsert`, carrying the
  three rules that were prose only: keys are `namespace.key`, blank clears,
  omitted is untouched.
- **F6** — field tables on all nine. The four `get` reads document a dynamic map
  with `fieldWithPath("*")` and the four `list-locales` a root array with
  `fieldWithPath("[]")`; both were verified by running rather than assumed.
- **F7** — nine operations: the definition-first 404 on all three owner types, the
  403 that states the role line once, two create 409s, the publish 409, the
  last-field 409, and the not-translatable 422.
- **G** — the false 409 is gone. That section now says the opposite and says it
  plainly: nothing protects the delete, it cascades to every value on every
  resource, and a confirmation step belongs in the client.

**Two things the strict check found that the audit had not.**
`metafield-definitions/get` returns **two fields the list projection omits** —
`description` and `updatedAt` — so the list's table was not a complete model of the
record. And `PublishedExamplesAreHonestTest` caught three of the nine new errors
publishing their happy path's request verbatim: all three were **pre-existing
assertions** taught to publish, and reusing the fixture is what teaching them to
publish does by default. Each now varies on what the error turns on — a
second entry for the publish 409, a differently-named definition for each create
409.

**Verified by mutation.** Adding `BOOLEAN` to `isTranslatable()` moves the
published translatable list in **six** files, and the only failure is
`UpsertMetafieldTranslationsUseCaseTest.aNonTextTypeIsRefusedByName` — the
behavioural test whose job is to pin the rule. Same arrangement as the menu depth
cap: the documentation derives, and one test still makes a human look.

---

# `touroperator` — 2026-08-17

Forty endpoints across twelve controllers — the largest context in the
application. **A–E clean**: forty mappings, forty snippet directories, forty
macros, and every field table complete enough that this context adds nothing to
the no-field-table list.

**Not one of the forty operations publishes an error.** Twenty-six non-2xx
assertions already sit in the twelve tests, and a reader can see none of them.
That is the whole finding, and it is the largest gap the series has met.

## Endpoint table

All forty have a test, a snippet and a macro, so the per-row columns would read
`yes yes yes strict` forty times. What differs is below.

| controller | endpoints | what is missing |
|---|---|---|
| `TourOperatorController` | 3 | create's side effects; update's request table is 2 of 6 fields |
| `TeamMemberController` | 4 | path variables; the ownership-transfer consequence; every error |
| `TeamInvitationController` | 5 | path variables on 2; every error |
| `InvitationAcceptController` | 2 | `{token}`; five distinct failures, including the API's only 410 |
| `MenuController` | 6 | path variables on all six; the duplicate-handle 409 |
| `PolicyController` | 5 | every error |
| `PolicyTranslationController` | 3 | every error |
| `BrandController` | 2 | three request and three response fields typed `Null` |
| `OperatorSeoController` | 2 | every error |
| `OperatorTranslationController` | 4 | every error |
| `StorefrontPasswordController` | 2 | path variables; the enable-without-password 422 |
| `TourOperatorLocalesController` | 2 | every error |

## F1. Promoting a member to OWNER demotes the caller, and nothing says so

`PATCH /members/{userId}` with `{"role":"OWNER"}` promotes the target **and
demotes the acting owner to ADMIN**, in one transaction. The caller cannot undo
it: only the new owner can transfer back.

Three places describe this endpoint and not one states the consequence. The
published `role` description reads "Target role: OWNER (ownership transfer),
ADMIN, or STAFF". The guide says "Promoting to OWNER is an owner-only ownership
transfer." Both name the *permission* and skip the *cost*.

- **Severity**: highest in this context. It is irreversible, it is one field
  value away from an ordinary role change, and an admin UI built from this
  documentation would offer OWNER in the same dropdown as ADMIN and STAFF.
- **Verified by**: `ChangeMemberRoleUseCase:117-133` — `transferOwnership` calls
  `caller.changeRole(MemberRole.ADMIN)` before `target.changeRole(OWNER)`, and
  the audit entry carries `demotedUserId`.

## F2. The invitee's two public routes publish only success

`/api/invitations/{token}/preview` and `/accept` are the only unauthenticated
routes in this context, and they are the flow an invitee actually walks. Five
failures, none documented:

- **404** — an unknown token.
- **409** — already accepted.
- **410 Gone** — revoked, or past its window. **No operation in the guide
  publishes a 410**; the status-code table names it and nothing shows it.
- **403** — a logged-in caller whose account email differs from the invitee's.
- **422** — accepting anonymously without `name` and `password`.

And the one an invitee hits routinely: **409, "An account with this email
already exists — log in to accept the invitation"**. Someone who already has a
Vointika account, clicked the link while logged out, and filled in the form gets
this. A frontend that has not been told about it shows a generic failure on the
most common path through the flow.

- **Verified by**: `AcceptInvitationUseCase:85-142` and
  `GetInvitationPreviewUseCase:42-45`; `GlobalExceptionHandler:55-56` maps
  `GoneException` to 410.

## F3. `tour-operators/update` publishes 2 of its 6 fields

`UpdateTourOperatorInput` carries `name`, `address`, `phone`, `email`,
`timezoneId` and `currencyId`. The fixture sends `phone` and `email`, so strict
`requestFields` passed on a two-row table — the rule this series recorded after
`experiences/create`, in a second place.

Two consequences, and the second is worse than an omission:

- **`address`'s whole-replace rule is published nowhere.** Sending
  `{"address":{"city":"Barcelona"}}` against a Madrid address replaces the whole
  object, so the street disappears. `AddressInput`'s javadoc calls this out —
  "a wrong address rather than a partial one, and one that looks entirely
  plausible" — and no reader of the guide sees it.
- **The guide describes a field the contract omits.** Its Update section spends a
  paragraph on `timezoneId` and what changing it does to stored departures.
  `timezoneId` is not in the published table at all.

- **Verified by**: `UpdateTourOperatorInput` read in full;
  `TourOperatorControllerDocumentationTest:200-212` sends two fields; the
  generated `tour-operators/update/request-fields.adoc` has two rows.

## F4. Twenty-one endpoints are role-gated and the context asserts no 403 anywhere

`grep -c isForbidden` across all twelve test classes returns zero. Twenty-one of
the forty use cases call `ensureAdmin` or `ensureOwner`.

The role split *is* this context — teams, invitations, brand, policies, the
storefront gate — and the boundary is invisible. The guide's prose carries it per
section, which is better than nothing and is not machine-readable.

## F5. Sixteen operations document no path variable

Every menu operation (6), every member operation (4), both storefront-password,
both `tour-operators/invitations` collection routes, and both public
`/api/invitations/{token}` routes. `tour-operators/create` correctly has none —
it is the only endpoint here with no path variable.

`{token}` is the one that matters: it is the emailed capability, and the two
routes that take it say nothing about it.

## F6. Nine fields publish type `Null`

`brand/get` and `brand/update` each publish three media ids that way,
`tour-operators/get` publishes `address.address2`, and `acceptedAt` is `Null` on
both invitation reads. Three of the nine are in a **request** table, which is why
the earlier count read six — the scan was looking at `response-fields.adoc`
alone.

A client reads `Null` as "this field can only ever be null" and skips the media
picker for three of the four brand images.

## F7. `Create Tour Operator` is one sentence over three hidden side effects

The guide says the caller becomes OWNER. Creation also:

- **generates the handle** from the name — the storefront subdomain, returned
  nowhere in the 201 (only the id, in `Location`);
- **generates a storefront password and enables the gate**, so the new store
  answers the password page rather than the shop;
- **creates two menus**, `main-menu` and `footer`.

A client that creates a store and opens its address gets the gate and no
explanation. A client that then creates a menu called `main-menu` gets a 409 for
a menu it never made.

Also undocumented: **409 on a second operator with the same name under the same
owner**, and 422 for an unknown country, timezone or currency.

- **Verified by**: `CreateTourOperatorUseCase:139-192`.

## G. Prose drift — none

Every hand-written claim in the forty sections was checked against its use case
and holds:

- resend and revoke "accepted or revoked → 409, a lapsed pending one can" —
  `TourOperatorInvitation:94-118`, where `requirePending` raises it;
- brand "an absent field clears it and absent collections empty them" —
  `UpdateBrandUseCase:35-45`;
- storefront password "a null or blank password keeps the stored one" —
  `UpdateStorefrontPasswordUseCase:53`;
- locales "unknown code or primary ∉ supported → 422" —
  `UpdateOperatorLocalesUseCase:57-67`;
- policy "the type is not settable" — `UpdatePolicyInput` has no `type`.

One near-miss, named because the series has met its shape before and it is not
drift: **`PATCH /locales` requires both fields.** Both are read unconditionally,
so sending one is a 422 — the `pages/update` trap. The guide's "Replaces the
primary + supported set" conveys it; the two field descriptions do not.

## H. Description quality — none

Checked mechanically across every `*-fields.adoc` in the context: no description
equals its field name, or its field name with "The" in front.

## Out of scope, noted once

Every test in the repository writes the tenant path variable as `{id}` while the
API declares `{tourOperatorId}` — 208 sites, so every published path-parameters
table names `id`. The name never reaches the wire, so nothing is wrong for a
client; it is a cosmetic mismatch with the guide's own prose, repo-wide rather
than this context's, and changing 208 call sites inside a documentation pass
would bury the pass. Left alone deliberately.

## What was fixed

Suite **1245 → 1253**; operations **188 → 210**. All twenty-two new operations are
errors, so this context went from publishing none to publishing the whole shape of
how it refuses things.

- **F1** — the `role` description and the guide both say it now: OWNER is a
  transfer, it demotes you to ADMIN in the same transaction, and only the new
  owner can hand it back. Plus `members/change-role-forbidden` (an admin may not
  touch the owner), `members/change-role-conflict` (never your own role) and
  `members/remove-conflict` (the last owner stays).
- **F2** — `invitations/preview-gone` publishes the API's **first and only 410**,
  under a heading saying why it is not a 404: 410 means ask for a new invitation,
  404 means retry. `invitations/accept-conflict` publishes the routine failure —
  you already have an account, so log in and return to the link — and
  `invitations/accept-forbidden` the wrong-email refusal.
- **F3** — the update fixture sends all six fields, so the table goes from 2 rows
  to 12. `address`'s whole-replace rule and `timezoneId`'s wall-clock rule are in
  the contract now, not only in prose the table contradicted.
- **F4** — `policies/create-forbidden` publishes the 403 once, under a heading
  that states the rule for all twenty-one gated endpoints: STAFF reads everything
  and writes nothing, and it is a 403 rather than a 404 because the caller *is* a
  member. The other 403s are the same answer and are not duplicated.
- **F5** — `pathParameters` on all sixteen, `{token}` included.
- **F6** — the nine are typed, and **the repo-wide count is zero**. Verified with
  the scan in the header block, not with the six the first pass found.
- **F7** — the Create section names the three side effects: the generated handle,
  the store arriving password-protected, and the two seeded menus. Plus
  `tour-operators/create-conflict` for the duplicate name, and a
  `menus/create-conflict` heading connecting the two — `main-menu` collides for an
  operator that has never made a menu.
- **The near-miss under G is closed**: both `locales` field descriptions say they
  are required on every call, and the guide says the PATCH replaces the pair.
- Also published, from assertions that already existed and showed nothing:
  `policies/create-conflict`, `policies/get-not-found`,
  `policy-translations/upsert-not-found`,
  `policy-translations/upsert-unsupported-locale`,
  `translations/upsert-unsupported-locale`, `brand/update-invalid`,
  `seo/update-invalid`, `storefront-password/update-invalid`,
  `locales/update-invalid`, `locales/get-not-found`,
  `invitations/create-not-found`, `members/list-not-found` and
  `menus/replace-items-invalid`.

**`menus/replace-items-invalid` could not have produced its own error**, which is
the `slots` lesson repeating. The assertion sent `{"items":[]}` while stubbing a
"nested at most 3 levels deep" refusal — an empty array would have succeeded. It
sends a four-level tree now. An error example that cannot cause the error is worse
than none, because a reader copies it and it works.

**Eleven of the twenty-two errors had to be varied away from their happy path** to
satisfy `PublishedExamplesAreHonestTest`: a distinct operator id for each tenant
404, a missing policy id, a STAFF token for the 403, a second title for the 409.
Every one was varied while writing rather than after the guard fired — which is
what having the guard is for.

**Everything above was checked in `target/generated-docs/api-guide.html`**, not
only in the snippets: 210 macros, 210 snippet directories, every one rendering a
curl example, and the no-field-table list still at 9 and still entirely
`metafield`.

### Review round 2 — six examples that were wrong rather than missing

A second pass traced every published error example back through its use case to
ask whether the request and response shown can occur together. **Two could not**,
and the suite could not see either: every documentation test mocks the use case it
documents, so a stubbed message publishes beside whatever request is sent.

- **`members/change-role-conflict` published `{"role":"OWNER"}` against "You cannot
  change your own role".** Neither caller who could send that gets that message.
  `execute` gates on the role *before* the transaction, so an ADMIN is refused at
  `ensureOwner` (403) and never reaches the self-check, and an OWNER is the sole
  owner by the single-owner index, so `apply` throws the other variant — transfer
  ownership first. The body is `{"role":"ADMIN"}` now, which is the only shape that
  reaches it.
- **`menus/create` published `main-menu` as a 201**, four sections above new prose
  saying that handle is seeded for every operator and always collides. Both could
  not be true. The prose was the accurate half, so the fixture moved to `legal` —
  which also makes the 201/409 pair differ on the **handle**, the field the conflict
  turns on, rather than only on the title.
- **`invitations/accept-conflict` called `name` and `password` "ignored on this
  path".** They are validated *before* the account lookup, so omitting them is the
  422 this same report lists as a separate failure — the two halves of the pass
  disagreed. They say "required even here" now.
- **`seo/update-invalid` reused the happy path's `MEDIA_ID`** while calling it
  another operator's, so one id was published as both owned and foreign two sections
  apart. Its sibling `brand/update-invalid` had introduced a distinct id for exactly
  this reason; SEO now has one too. Same shape on
  `members/change-role-forbidden`, which PATCHed the same member id `change-role`
  uses while calling it the owner's.
- **`storefront-password/update-invalid` published "#157"** in a field table. An
  internal PR number means nothing to an API consumer; the adjacent prose already
  said "since the gate landed".
- **The `timezoneId` description reassured where the code warns.** It said changing
  the timezone keeps a 10:00 sailing at 10:00, which reads as *safe*.
  `UpdateTourOperatorUseCase`'s javadoc frames the identical mechanism as the
  hazard: the wall-clock is preserved and therefore **silently means a different
  instant**, nothing rewrites the rows, and a migration is the open half. The field
  description and the guide paragraph both say that now.

**The lesson, promoted to the repo-wide block**: differing from the happy path is
not the same as being reachable. `PublishedExamplesAreHonestTest` checks the first
and nothing checks the second, so it is read by hand — in the order the use case
runs its guards.

### Review round 1 — the depth cap was the 25 MB defect again

`MenuItem.MAX_DEPTH` was private and **six places wrote "3" out by hand**, four of
them published: the guide's prose, both item-tree descriptions, and the stubbed
422's message. Raising the cap would have enforced the new number while every
published place stated the old one, with a green build — nothing but the throw
site read the constant.

Closed the way #174 closed the media cap, plus one step further:

- `MAX_DEPTH` is public, with `tooDeepMessage()` beside it. The throw site, both
  descriptions, the doc test's stub and `MenuUseCasesTest`'s message assertion all
  derive from them.
- **The guide's prose stopped carrying the number instead of being guarded.** It
  says "nesting past the cap" and points at the generated table and the published
  error body. That is the resolution the `CAP_MB` javadoc already recommends —
  reword rather than exempt — and it needs no fourth prose guard.
- `ReplaceMenuItemsRequest`'s javadoc links `MenuItem#MAX_DEPTH` rather than
  repeating it.

**Verified by mutation, not by reading.** With `MAX_DEPTH = 4` the build publishes
"4 levels" in all four places and **zero** mentions of "3 levels" survive anywhere
in `target/`. One test fails under the mutation —
`MenuUseCasesTest.replaceRejectsTooDeepTreeBeforeAnyWrite`, whose fixture is a
fixed four-level tree that a cap of 4 now allows. **That one is left hardcoded on
purpose**: it is the behavioural check that the refusal still works, so whoever
raises the cap should have to look at it. Deriving its depth too would make the
mutation pass silently, which is the opposite of what it is for.

---

# `experience` — 2026-08-17

Sixteen endpoints across three controllers — the largest context yet. **A–E clean**:
sixteen mappings, sixteen snippet directories, sixteen macros.

**`slots` published no contract at all.** All six of its operations emitted only the
default snippets: no headers, no path variables, no field tables, no request bodies.
Six endpoints, and the guide showed a reader a curl and a raw JSON blob for each.

That matters more here than in the contexts where it happened before, because slots
carry the domain rules a client is most likely to get wrong.

## F1. The slot contract was entirely unpublished

Nothing said that `startAt` is **operator-local wall-clock with no zone** — the reason
a 10:00 sailing stays 10:00 when an operator corrects their timezone. Nothing said
`price` and `capacity` are **frozen per slot at create**, so editing an audience never
reprices a sold departure. Nothing said `durationMinutes` is derived, or that `day` is
Sunday-first, or that **none of the three statuses is an operator toggle** —
`SOLD_OUT` is counted from bookings, `CANCELLED` has its own endpoint, and the PATCH
edits capacity only.

## F2. Four error assertions existed, none published — and two more rules had none

Asserted and unpublished:

- **409 — a cancelled slot is terminal.** It cannot be re-cancelled, edited or
  reopened; you recreate it. The guard is asked once where the edit begins, so the
  capacity PATCH inherits it.
- **422 — a recurring pattern whose window contains none of its days.** It would
  create zero departures, so it is refused rather than silently succeeding with
  nothing.
- **403** for a STAFF member, and **404** for a non-member.

Neither asserted nor published, and both raised by `UpdateSlotUseCase:93,97`:

- **422 — capacity may never go below `bookedCount`**, the one an operator reducing
  seats on a selling departure meets.
- **422 — a tier not priced on this slot.**

*An earlier revision of this section listed the capacity 422 among the four asserted
and omitted the 403, so the four found and the four fixed were not the same four.
Caught in review; both are covered now.*

## F3. `experiences/get` and both translation reads had no field table

And `experiences/update` — a whole replace — documented no request body at all.

## G. Prose drift — one, and this pass rewrote the section around it

**`Update a Slot` promised "Status and per-audience capacity".** Status is not
updatable, and four places say so — the `requestFields` table rendered directly
below it (which carries `capacities[]` and nothing else), the `status` description in
the response table on the same page, `UpdateSlotInput:9`, and this pass's own test
comment. `SlotStatus`'s javadoc records that status *was* briefly PATCH-writeable, so
the sentence looks like the survivor of removing it.

A client reads it, sends `{"status": "CANCELLED"}`, and gets a 400 for a body with no
such field, while `cancel` is its own endpoint two sections down. Pre-existing on
`main`, and this section reported the category clean while rewriting the tables that
contradict it.

## H. Description quality — none

## What was fixed

Six new operations, five of them errors; every slot operation now documents its
headers, path variables and body.

- **F1** — the sixteen-field slot response, with the wall-clock, frozen-pricing and
  status rules in the descriptions rather than only in Javadoc.
- **F2** — `slots/cancel-conflict`, `slots/create-recurring-no-match`,
  `slots/create-single-forbidden`, `slots/list-not-found`, plus the two that had no
  assertion at all: `slots/update-capacity-too-low` and `slots/get-not-found`.
- **Every slot example is now runnable.** All three create fixtures carried hardcoded
  dates that were future when written and past by the time anyone read them — so a
  reader copying the published request got `"Date must be today or later"`, and the
  recurring-422 example **could not produce the error it documents**, because the
  window check runs before the day match. The dates derive from `LocalDate.now()` now;
  the snapshots differ per build, which costs nothing because generated snippets are
  not in version control.
- **`slots/create-forbidden` was silently outside `PublishedExamplesAreHonestTest`** —
  the guard finds a happy path by longest name *prefix*, and no operation is a prefix
  of that name. Renamed to `slots/create-single-forbidden`, which engages it.
- **`SOLD_OUT` stopped being published as live behaviour.** The description said it
  *is* derived from bookings — present tense, in four operations. Nothing writes it:
  `grep -rn "SOLD_OUT" src/main/java` returns the enum declaration and two javadoc
  mentions, no assignment. It is counted at checkout success and checkout does not
  exist, so the only rows carrying it are the dev seed's. A client greying out full
  departures would have waited for a value the API cannot produce — and it appears to
  work against the seed, which is worse than never appearing. The description says
  **not written yet** now.
- **The singular/plural path trap is stated where a reader meets it.**
  `…/experiences/{id}/slot` creates one departure and `…/slots` creates a recurring
  pattern; the published curls differ by one character, and a client following REST
  convention posts a single-departure body to the collection path and gets a 422 about
  `days`. Both sections now name their path.
- **F3** — field tables on all three reads and a request body on the update. The
  tracked no-field-table list drops **16 → 9**, and every one of the nine that remain
  belongs to `metafield`.

**Both guards fired again, and both caught real work.**
`ApiGuideDocumentsEveryEndpointTest` found four operations with no `operation::` line.
`PublishedExamplesAreHonestTest` found three errors publishing their happy path's
request — including the recurring 422, which was sending **the pattern that produces
slots** while claiming to document the pattern that matches nothing.

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
unknown token and a **replayed** one. An expired token says so —
`"Refresh token has expired"` — so the ambiguity is two-way, not three. The sameness
where it exists is deliberate: telling a caller they tripped the reuse detector tells
an attacker the same thing.

The consequence is not the same. A replayed token is treated as a theft signal and
`revokeAllByFamilyId` ends **every session descended from that login**. Retrying a
refresh with a token already exchanged lands there.

**A genuine simultaneous double-submit does not**, and an earlier revision of this
section said it did. `:93` guards the rotation so that two tabs presenting the same
*live* token produce a plain 401 for the loser with the family intact — the comment
there says as much. Publishing the opposite would have had frontend authors put a
mutex around refresh to prevent a logout that cannot happen. Caught in review, from
this section's own `Verified by` line, which cited the guard three lines below the
claim.

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
- The avatar is now inside `ApiGuideNamesTheRealAllowlistTest` — **reading the guide**.
  The first attempt asserted against `SetAvatarUseCase` alone and never opened the
  file, while its failure message claimed the two disagreed; a sentence naming a
  refused type left the build green. It compares the Set Avatar prose against the
  allowlist now, anchored like its siblings.
- `LARGEST_APP_CAP` in `MultipartLimitsTest` derives from **both** caps rather than
  naming the media one and describing the avatar's in a comment, and
  `application.yml` stops restating either. Making the avatar cap public closed those
  two the same way it closed the published description.

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

## Body-returning operations with no field table — none left

**This list is closed.** It has read 71 (a bad scan), 20, 19 (blind to the
`withDefaults` set), 22 (corrected), 20, 17, 16 and 9, and `metafield`'s pass took
the last one. MAP's Debt entry pointing here can go with it.

The scan, for whoever needs it again — strip both fences, keep non-empty bodies,
test for a sibling table:

```
for d in $(find target/generated-snippets -name response-body.adoc | xargs -n1 dirname); do
  b=$(grep -v '^\[source' "$d/response-body.adoc" | grep -v '^----$' | tr -d '[:space:]')
  [ -n "$b" ] && [ ! -f "$d/response-fields.adoc" ] && echo "$d"
done
```

`sed '1d;$d'` is not enough — it leaves a `----` behind and reports every 204
endpoint as a gap.

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
