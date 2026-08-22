# Vointika Backend — Open Work

What is owed, what is wanted, what is settled, and what is still open. Carried
forward when `MAP.md` was deleted (2026-08-20) and split out of `CLAUDE.md` on
2026-08-21, because it is the half that moves every slice while the rest of that
file describes a shape that mostly holds.

**`CLAUDE.md` is still the state of the repo** — what exists and what each part
owns. This is the ledger beside it. Nothing belongs in both; if an entry here
becomes a description of what is built, it moves there and leaves nothing behind
(LAW §3).

**Debt** is owed. **Backlog** is wanted but unscheduled. **Decided** is settled and
is not to be re-litigated without a reason. **Open decisions** are the ones that
block coordination.

## Debt

- **No way to search a list, so the admin drains it** (2026-08-14, found by the
  frontend audit) — `ListConstants.PAGE_SIZE` is a fixed **20** and `ListQuery` has
  no limit field, so a picker that needs every row walks the cursor. An operator
  with 300 experiences waits on 15 chained requests before the availability dialog
  is usable, and four call sites do this (experiences ×2, pages, metaobject
  entries). The fix is a server-side search/typeahead endpoint. It belongs with the
  **body-returning endpoints that document no field table**, which is **closed as of
  2026-08-17**: the count went 20 → 0 across the API-docs series, `metafield` taking
  the last one. The scan that keeps it true is in `backend/PATTERNS.md` §9a. The
  frontend's own three findings from that audit stay in its repo.

- **The §4d cross-namespace guard is a pre-check, not a constraint** (2026-08-01,
  from the review of the `experience` fix) — affects `page` and `experience`
  equally. Uniqueness *within* a namespace has a unique index behind it, so a lost
  race is rejected; nothing spans `experiences.handle` and
  `experience_translations.handle` (or the page pair), and nothing can without a
  trigger. Two concurrent writes, one per namespace, can still land on the same
  value and shadow. Narrow window, no known occurrence, and the alternatives
  (trigger, or a shared handle table) are both larger than the exposure — recorded
  so the guards are not mistaken for completeness. PATTERNS §4d now says so.
- **`experience` upsert has no named 409 on a lost handle race** (2026-08-01, from
  the §4d fix). `page`'s translation upsert wraps its transaction in
  `catch (UniqueConstraintViolationException)` to name the conflict. Experience's
  does not, despite having the same partial unique index
  (`uq_experience_translations_operator_locale_handle`). **Cosmetic, not a status
  bug**: uncaught, the exception already maps to 409, so the only difference is
  the message ("A concurrent write already created this record" vs an
  operation-specific one). Left out of the §4d fix under LAW §6.4 rather than
  widened into it.
- **A slot date-range filter is not expressible** (2026-07-31, from the `shared`
  audit) — `ListSlotsUseCase` makes `startAt` **sortable but not filterable**, and
  `ListSchema` has no builder for `LocalDateTime` — the filter kinds are `text`,
  `set`, `number`, `bool`, `time`→`LocalTime` and `instant`→`Instant`, and the
  unused `date`→`LocalDate` one was deleted.
  So "slots in August" cannot be asked for — the single most obvious filter on a
  departures list. Needs a `localDateTime(...)` filter kind wired to the existing
  comparable predicate, which `ValueCoercion` already parses. Add it when a
  screen asks; do not re-add a filter kind nothing declares. It used to be half
  of a pair with the contact unread filter — **that half was dissolved in #130**
  by deleting read-state, so a null operator on the shared framework now has no
  caller asking for it.
- **The per-resource metafields read is an unbounded array** (2026-07-31, from the
  `metafield` audit; **scope widened 2026-08-15**) — **three endpoints**, not the two
  this entry named until the audit recounted them: `GET .../experiences/{id}/metafields`,
  `.../pages/{id}/metafields`, and `.../tour-operators/{id}/metafields`, the last added
  with the `TOUR_OPERATOR` owner type in #139. Each returns a bare
  `List<MetafieldValueResponse>` while the other three lists in the context all use
  the shared cursor framework. Its size is bounded only by how many definitions the
  operator has created for that owner type, and definitions are themselves
  cursor-paginated, so the bound is "whatever the operator built". Not fixed in the
  audit because it is a **wire-contract decision**, not a defect: the admin editor
  overlays every value onto the definitions form in one render, so paginating it
  makes the editor page. Either cap + paginate it, or record the exemption the way
  the reference lists have one.

- **On an `ERROR` dispatch the security chain answers 401, not the error**
  (2026-08-22, found while sweeping the admin API from a tenant host) — Boot
  registers the security filter for `ASYNC, ERROR, REQUEST`, so a request the
  container refuses before MVC is error-dispatched to `/error` and runs the whole
  chain a second time. `/error` is in no `PublicRoute`, so an unauthenticated
  caller gets `401 Authentication required` in place of whatever actually went
  wrong. `/api/tour-operators//experiences` is a live example: the double slash is
  refused, and the caller is told they are unauthenticated.

  **Pre-existing and unrelated to the storefront**, which is why it is filed rather
  than fixed in the slice that found it. `StorefrontUnauthenticatedRequests`
  deliberately declines non-`REQUEST` dispatches so it does not *change* this — an
  error response belongs to whatever produced it — but declining restores the 401
  rather than improving on it.

  **Not urgent, and the reason is worth stating**: an MVC-handled error is
  unaffected (`GlobalExceptionHandler` writes its own body — a malformed cursor
  still answers a real 500), so this only bites requests rejected in the filter
  chain, which today means malformed URIs. The fix is either a `PublicRoute` for
  `/error` or narrowing `spring.security.filter.dispatcher-types` to `REQUEST`,
  and the second needs checking against what else the chain does on those
  dispatches before it is taken.

*Audited 2026-07-21: no TODO/FIXME/HACK/stub markers, no orphan fallback code, no
dead code, no hidden `@SuppressWarnings` hacks (the Kafka raw-type ones are the
deliberate Boot-4 injection). Reference/ui-language plain-array lists are curated
& bounded — intentionally exempt from §4b, not debt.*

## Backlog

Known wants, not yet scheduled — deliberate future work, not shortcuts.

- **A cursor-paginated storefront listing indexes only its first page**
  (2026-08-21, from the design of the experiences listing; **the listing shipped
  the same day, so this is now live rather than prospective**) — still Backlog,
  because all four conditions that make it bite are absent and the cause will be
  invisible once they are not.

  `canonicalUrl` is built from the resolved path with the query string **stripped**
  — deliberately, and pinned by `theCanonicalUrlDropsTheQueryString`, because
  echoing the URI back would repeat `?utm_source=…`, which is the one thing the tag
  exists to remove. A keyset cursor rides in that query string. So
  `/experiences?cursor=eyJz…` publishes `canonicalUrl` pointing at
  `/experiences` — page one's address.

  A canonical is an instruction, not a hint. A crawler on page two is told its real
  address is page one, treats it as a duplicate and drops it, so **only the first
  `PAGE_SIZE` experiences are ever indexed** and the rest have no address search can
  hold. The opaque cursor compounds it independently: a crawler can only *follow* a
  cursor we render and never construct one, nobody can link "page 3", and a
  bookmarked cursor URL rots when the row it keys on is deleted. Shopify's `?page=N`
  has none of those properties — stable, guessable, and each page's canonical
  self-references *that* page.

  **Nothing is owed yet and that is why this is Backlog.** We serve JSON, nothing
  emits a `<link rel="canonical">` — the field is data for a theme that does not
  exist — no storefront is deployed, and no operator is near twenty experiences. It
  becomes real when all four change at once.

  **What shipped makes it concrete rather than hypothetical.**
  `/experiences?cursor=…` is a live address today and its `canonicalUrl` really is
  `/experiences`. The seeded operator has five experiences against a page size of
  twenty, so page two does not yet exist to be dropped.

  The fix is page-number paging on the public listing, or a canonical that carries
  the paging parameter. **Decide it when themes land**, and note the shape of the
  argument is the same one behind handle history and 301s below: a public URL is a
  promise, and the admin's list framework was never designed to make one.


- **Where per-page-type SEO text lives** (2026-08-03, #92 — **was filed as Debt
  until 2026-08-11**) — every storefront page falls back to the operator's
  `seo_title`/`seo_description`, and the experiences listing has no entity of its
  own to carry one. The home page is fine (the shop *is* its subject); a listing
  sharing the shop's title is already duplicate-ish, and every further page type —
  a collection, a search — inherits it. **Nothing was invented for it in #92 on
  purpose: a schema decided by a template is the wrong order.** The options are a
  `page`-like row per page type, theme settings, or nothing.

  **Corrected 2026-08-21.** This entry said to settle it *"before the experience
  detail page ships its own answer, which is the first thing that would force
  one."* The detail page shipped and forced nothing: `experiences` and their
  overlay have carried `seo_title`/`seo_description` since experience/V8, so it
  uses the same `SeoText` chain a CMS page does. The entry's real subject is page
  types with **no entity to carry SEO** — the listing, which already shipped
  without an answer, and whatever comes next. It no longer has a slice attached to
  unpark it.

  **A third case appeared 2026-08-22 with the policy page, and it is the cheap
  one.** A policy *has* an entity and a translatable title, but no SEO columns —
  so `SeoText.title(null, policy.title(), …)` gives it a real per-page title with
  no schema at all, and only the description falls through to the operator's.
  That is deliberately where it was left: a policy document's own first paragraph
  is not a meta description, and inventing `seo_description` on
  `tour_operator_policies` would be a column added for a template. Worth noting
  because it narrows the open question to page types with **neither an entity nor
  a title**.

  **That set is now two**: the experiences listing, and the contact page
  (2026-08-22). Contact makes the shape plainer than the listing did — a page
  whose whole subject is one word, which nobody can hardcode because "Contact"
  is English and the storefront serves several languages. So it takes the
  operator's title, and the first storefront with two published locales will show
  the same `pageTitle` on the shop, the listing and the contact page. That is the
  cost, stated rather than discovered.
- **Should a storefront card show that an experience is featured?** (2026-08-03,
  #92. **Filed as Debt until 2026-08-11, where it did not belong.**) `featured`
  already orders the listing, featured rows first. The page just says nothing about
  it, so a visitor cannot tell which ones the operator promoted.
  Nothing is owed and nothing is half-built: the card component that carried the
  flag was **dropped in review** under LAW §2.4 rather than parked in a shared
  port waiting for an answer, precisely because nothing read it. What is open is
  a **product question**.

  **Narrowed 2026-08-21.** The detail page serves `experience.featured`, so the
  backend half is done and the field is no longer hypothetical. What remains is
  purely whether a theme renders a badge, and whether the *card* on the listing
  needs the flag too — the card still does not carry it.

- **Experience `type`** (2026-08-12; `category` shipped 2026-08-21, see *Decided*
  below — **`type` is still wanted and still unscheduled**). Before categories an
  experience carried no classification at all: `tags` was dropped in V10, and
  `audience` is who a slot is priced for, not what kind of thing an experience is.
  That is why `shop.types` was filed as "nothing to expose" rather than as a
  contract gap — there is still no column behind it.
  **They are different in kind, which is why Shopify keeps both.**
  `product.type` is **free text the merchant types** — flat, unvalidated,
  per-store, and it drifts ("Boat tour" / "boat tours" / "Boat Tours" become
  three). `product.category` is a node in a **published taxonomy**:
  `taxonomy_category` carries a hierarchical id (`hb-1-9-6`), a **localized**
  `name`, and `ancestors` for a breadcrumb. There is deliberately **no
  `shop.categories`** — the taxonomy is the same for every store, so only the
  per-store invention is worth enumerating.
  **So they were two slices, not one, and only one has landed.** `type` is a
  nullable column on `experience` plus one `SELECT DISTINCT` behind
  `tourOperator.types` — still unbuilt. What shipped as `category` is **not** the
  curated `reference` tree this entry proposed; see *Decided* below for what was
  built instead and why. **Whether a global curated taxonomy is still wanted
  alongside it is genuinely open** — nothing here needs one until marketplace
  sync or a cross-operator search does, and neither exists. `type` does not block
  the storefront contract; it changes it when it lands.

- **Member notification-subscriptions** — `/me/notification-subscriptions`,
  personal per-alert-type prefs (was briefly "next slice" before the booking
  loop took priority).
- **List-my-pending invitations** — a `/me` view of invitations addressed to
  the caller, cross-operator (SPA onboarding; distinct from the operator-scoped
  list already shipped).
- **Handle history + 301s** — renaming an experience handle or a page handle makes the
  old URL 404. Deferred once at #36 and again at #56, and the storefront has now
  made it externally visible: shared links and indexed results break silently.
  Needs a history table and a redirect on miss.
- **Metafield v2** — per-definition validations and choice lists, plus the
  `list.*` and `rich_text` types. Scoped while #57–#60 were built and never
  scheduled; the operator-level owner type and storefront read access were part of
  it and have both since shipped.
- **Locale `is_published`** — `tour_operator_locales` shipped without it (#32),
  deferring the question "to the storefront". The storefront exists now and treats
  every *supported* locale as published, so the idea is either dead or a real gap:
  today an operator cannot prepare a translation without exposing it. Decide.
- **Structured JSON logging** — today logging is plain text → stdout with MDC
  correlation (`requestId`/`userId`, propagated across the Kafka hop). For prod
  log aggregation, add a JSON encoder (e.g. a `logback-spring.xml` with an
  ECS/logstash encoder) behind a profile, preserving the correlation fields; keep
  dev human-readable. Non-urgent.
- **Country flag assets** — `country.flag_key` exists (**nullable since reference/V6**, `flags/{iso2}.svg`)
  and resolves to `flagUrl`. **The bucket question is settled and three flags
  exist**: ES/US/DO ship in `docker/dev-seed/flags/` and `minio-init` uploads them
  to `avatars/flags/`, the same public base media uses (#146). What is left is the
  other 246 — a full SVG set, a production upload path (`minio-init` is dev only),
  and then one `UPDATE reference.country SET flag_key = 'flags/' || lower(code) ||
  '.svg'` plus a re-tightening `SET NOT NULL`. Still its own small slice.

## Decided

- **Repo shape** (2026-07-19) — **separate repo per project**: backend, admin,
  storefront (and themes) each own their own git repo, as before. *Not* a
  mono-repo. LAW therefore lives at `/home/jefrycayo/vointika/` root — the
  cross-repo spine, mirroring the archived `CONTEXT.md` precedent. **Amended
  2026-07-31:** that root is itself a git repo (the three project repos are
  `.gitignore`d, so the separate-repo-per-project decision is untouched). It had no
  history, no diff and no backup, and LAW §0.4 requires an amendment to be
  accounted for "in the commit" — impossible while nothing tracked it. **Amended
  2026-08-20:** it tracked LAW *and* MAP until MAP was deleted; per-repo state now
  lives in each repo's own `CLAUDE.md`, which is what the section above is.
- **Events on Kafka** (2026-07-19) — the `EventPublisherPort` seam is backed by a
  Kafka producer; contexts produce/consume over Kafka, never direct imports.
  **Topic convention:** `<producing-context>.<event-kebab>`, one topic per event,
  keyed by recipient email. Kafka client fenced to `shared.infrastructure.kafka` +
  the notification consumer (ArchUnit).
- **The dev stack's shape** (2026-07-19) — Kafka is KRaft single-node, no
  Zookeeper. SES runs against **`aws-ses-v2-local`**. LocalStack was rejected: its SESv2
  `SendEmail` retrieval is broken (`/_aws/ses` KeyError 'Source') and paywalled.
  Reading the sent mail is the whole point, since the verify and reset tokens leave
  only by email. Versions and endpoints are in `backend/STACK.md`. Without AWS
  credentials the async send logs a failure and the flow still completes.
- **Email locale rides the event** (2026-07-20) — identity emails are sent in the
  recipient's language. Each identity event carries a `locale` (set from
  `User.language` at publish); the notification consumers pass it to
  `SendNotificationUseCase` (exact→subtag→en fallback). notification never queries
  identity. Templates ship per-locale on the classpath (en + es), tracking
  `app.identity.ui-languages`. **Register captures the language** as an optional
  **body field** (`POST /register {language}` — the FE's Paraglide locale),
  validated against the allowlist, stored on the new user; unsupported/absent →
  `en` (never fails a signup). Not a header — only register captures; it's a
  persisted attribute.
- **UI languages are not reference data** (2026-07-20) — a UI language is a
  locale code, not a row with attributes. Model = `User.language` (the choice) +
  `app.identity.ui-languages` config allowlist (validation) + **authenticated**
  `GET /api/ui-languages` (the picker's single source of truth; codes only,
  labels via `Intl.DisplayNames`). **`reference.languages` is a different list** —
  it is the *content*-language allowlist an operator publishes in, not the admin
  UI's. Grow a
  language = add the yml code + ship the FE catalog + **ship the email templates
  and add the code to `ClasspathTemplateCatalog.LOCALES`**; no migration.
  **Corrected 2026-08-01** (the `notification` audit): this entry, PATTERNS §8 and
  two Javadocs all said "zero code", and email was the exception nobody had
  counted — a language on the allowlist with no templates sends in English rather
  than failing. `TemplateLocalesTrackUiLanguagesTest` now fails the build on
  divergence.

- **A column lands with the feature that reads it** (2026-07-20) — the rule the
  `tour_operators` row was created under, and the reason it still has **no `status`
  or fee field**: a lifecycle column arrives with whatever reasons about it. The row
  has grown a great deal since (brand, locales, the password gate, a structured
  address) and every one of those came with its reader.
  **The welcome email is sent through the standard path** — event → notification
  consumer → SES, in the creator's UI language — which is why a `touroperator` event
  exists at all; it was deferred while nothing consumed it. Recipient
  email/name/language is resolved at publish time via `UserAccountQuery.findContact`
  and rides the event, so the consumer never queries identity.
  A generic `TourOperatorCreated` event for storefront initialization is still **not**
  built: the storefront context exists now, but nothing in it needs to react to a
  new operator.

- **What an experience's "from" price means** (2026-08-01) — `starting_price`
  (experience/V7) sits on the row the card already loads, because rendering may not
  fan out to per-slot prices. Two rules are decided: **nothing scheduled → 0**, and
  the figure is **per person**, not per `paxPerUnit`. Free tiers are excluded, which
  is what makes 0 safe rather than a lie — a real starting price is never 0, so 0
  means "nothing priced yet" and the card hides the badge. **Which slots count is
  still open**: `slots.status` is AVAILABLE/SOLD_OUT/CANCELLED and departures are
  dated, so a naive MIN would quote a cancelled or past one. It is operator-set
  since #119, so nothing derives it today. The next departure is a separate
  question and stays out.
- **Categories are the operator's, not the platform's** (2026-08-21) — the four
  calls that shaped the slice, recorded because each one was a fork and the
  Shopify research pointed the other way on the first.

  **Operator-owned with full CRUD**, not a curated `reference` taxonomy. The
  backlog entry above proposed the latter, from Shopify's model. Shopify's three
  reasons for a global taxonomy — tax rates, marketplace sync, and unlocking
  category attributes — **are all absent here**: no tax engine, no channels, and
  our metafields are operator-authored rather than derived from a category. So a
  curated tree would have been a large slice buying navigation alone, and their
  data does not transfer anyway (it classifies goods; Travel & Leisure is
  transport tickets, not "sunset kayak tour").

  **Flat, not a tree.** `ancestors` only earns its keep once something renders a
  breadcrumb, and nothing does.

  **Delete sets the reference null** rather than refusing while in use or
  cascading. An uncategorized experience is the state it was in before anyone
  filed it, so the classification is the only thing a delete destroys. Verified
  against the live database, not reasoned: deleting a seeded category left all
  five experiences present and moved two to uncategorized.

  **Inside `experience`, not its own context.** A category has no lifecycle apart
  from the experiences it classifies. Being in the same context is what makes the
  FK a plain intra-schema one and costs no shared port — the mirror of why
  `audience` *did* earn a boundary (it is reused across slots, and `experience`
  reaches it through `AudienceOwnershipQuery`).

  **No handle, deliberately.** A handle is a permanent URL and nothing routes to a
  category. It lands with the storefront page that serves one — and walks into the
  handle-history/301 gap already carried above when it does.

- **The storefront's JSON is a render context, not a published API** (2026-08-22,
  answered by Jefry the day it was asked) — **it is not an open API**. The JSON
  exists so the data can be read and debugged quickly while the contract is being
  got right; it is the object model a Mustache template will be handed, and
  nothing else.

  Three consequences, and the first is why answering early was worth it:

  - **A field rename is not a breaking change to anyone.** There is no second
    audience upgrading on its own schedule. §2a's warning — that renaming a
    component is breaking once operators author themes — starts applying when
    **themes** exist, not now, so the contract can still be corrected freely.
  - **Any AJAX surface is designed on purpose rather than inherited.** The cart,
    search-suggest and paging endpoints get shapes chosen for them, which is what
    the storefront-order section means by AJAX being three problems. Shopify keeps
    `/products/x` and `/products/x.js` as *different shapes* for this reason.
  - **The JSON has no compatibility claim on it**, so it may stay as a debugging
    view alongside templates, or go, without that being a decision anyone owes.

  This closed open decision 4. It does **not** decide what §2a's contract looks
  like — only who it is for.

- **The contact page is a route, not a Shopify-style page template** (2026-08-22,
  chosen by Jefry against the recommendation, which is why the alternative is
  written down here rather than lost).

  **Shopify has no `/contact` page.** A merchant creates an ordinary CMS page and
  assigns it the `page.contact` template; the URL stays `/pages/<handle>`, and
  only the form's *submission* goes to `/contact`. The template-marker version of
  this would have been a nullable `template` column on `page.pages` surfaced as
  `page.template`, keeping one contact page with the operator's own authored copy
  above the form — and generalising to any future custom template.

  **What was chosen instead**: `/contact` and `/{locale}/contact` as a sixth route
  shape with `pageType: "contact"`. No migration, no admin write path, and the
  storefront gains a page without `page` gaining a concept.

  **The consequence to know, because it is already real**: the seeded operator has
  a published CMS page at `/pages/contact` whose body is exactly the copy that
  would sit above a form — *"Get in touch. Email hello@acme.test or find us at the
  Old Port kiosk from 9:00."* That page still exists and still serves. So an
  operator has two contact addresses and no way to say which is canonical, and the
  authored copy is stranded on the one without the form. If that becomes a
  complaint, the fix is the template marker above, and `/contact` would then be
  either a redirect or a deletion.

- **Invitation model** (2026-07-20) — invitations key on **email** (invitee may
  have no account). Raw token only in the emailed link; **SHA-256 hash at rest**
  (`token_hash` unique). 7-day expiry judged **lazily** on access (no job). At most
  one PENDING per (operator, email) — DB partial unique index. Invite = ADMIN+;
  invitable roles = ADMIN/STAFF only (**OWNER is transfer-only**, 422). Accept is
  **public** (token is the capability); anonymous accept provisions a
  **verified-from-birth** user via `InvitedUserProvisioning` + issues a session
  (auto-login), and **fails closed 409** if the email already has an account
  (pre-registered-email attack). Invite email sent in the **inviter's** UI language
  (invitee has none yet). **All four invitation actions are audited**, entity type
  `INVITATION` — `member.invited` (not `invitation.created`; the invite is a team
  action), `invitation.accepted`, `invitation.resent`, `invitation.revoked`. This
  entry said audit was "subtracted (no audit context yet)", written six days before
  #54 built one and swept these in with the other 28.

- **An operator may change their timezone, and departures keep their wall-clock
  hour** (2026-08-08, #108) — **asked for by clients**, so it is a requirement
  rather than a tolerated side effect.
  Slots store operator-LOCAL wall-clock times (`LocalDateTime`, no zone), so
  changing the zone leaves every stored departure at the hour it was authored: a
  10:00 sailing stays a 10:00 sailing, and only its absolute instant moves.
  **That is the right behaviour for the case that actually happens** — an
  operator correcting a timezone they set wrongly, or one whose business moved.
  Their tours always ran at 10am local; the zone was the thing that was wrong.
  Rewriting the rows to preserve the absolute instant would do the opposite,
  moving every departure to a time nobody scheduled.
  So **nothing migrates the slots, by design**, and this is deliberately not
  carried as debt. Two things follow, recorded so they are not rediscovered as
  bugs: `CreateSlotUseCase`'s past-date guard starts judging against a different
  today, and the change is audited with the before/after zone. Revisit only if
  bookings ever need the absolute instant preserved for an already-sold
  departure — which cannot arise yet, since the transaction half is unbuilt.

## Open decisions

Coordination-critical and unresolved. Resolve deliberately; record the outcome
in **Decided** above when closed.

### The order the storefront is being built in

Stated 2026-08-22, and it is what makes the *Shopify OS 2.0 scope* and *cart
cookie* decisions legible together: **the data contract first, then rendering,
then formatting** — objects, then templates, then money and dates. Every
storefront slice so far has been the first of those, which is why it serves JSON
and not HTML — and **that JSON is a render context, not an API**, settled the same
day and recorded under *Decided*.

(Referenced by name, not by number. The numbered list below renumbers whenever an
entry closes, and it has already done so once — the *published API* question was 4
until it was answered, which silently made two pointers here wrong.)

Alongside them sit filtering and AJAX. **AJAX is three problems and must not be
planned as one**, because they have different security postures:

- **read-side** — facets, search-suggest, pagination without a reload. Same data,
  different transport; nearly free if the contract is right, and it is what forces
  the cursor-versus-page-number question filed in Backlog.
- **the cart** — stateful, cookie-identified, and the first *public writes* this
  application has ever had. The *cart cookie / CSRF posture* decision below is
  its prerequisite.
- **checkout** — payments, a different risk class again, and where §7a's
  "critical-event delivery — decided, unbuilt" finally gets its trigger.

**The cart comes after rendering, not before.** A cart with no page to sit on is a
JSON API for nobody, and its shape depends on what a theme's add-to-cart form
actually posts.

1. **Context collapse.** Which of the old ~19 contexts survive, merge, or die.
   **Still to place: cart · theme · payment · sales** — and none of them can be
   settled before the transaction arc exists, so this decision is only as open as
   that work is unbuilt.

   Thirteen are placed and built: identity · notification · reference ·
   touroperator · media · experience · audience · pickup · audit · page ·
   metafield · contact · storefront, on the `shared` kernel. Three of those
   placements were close calls and are the precedent for the four remaining:
   **metaobjects did not earn a context.** They live inside `metafield` as one
   custom-data context rather than becoming a fifteenth. **`menus` and the
   storefront password went into `touroperator`**, because one storefront per
   operator makes the operator the scope. And **`storefront` did earn one**
   (2026-08-03), on lifecycle: it is public, unauthenticated and eventually HTML,
   against `touroperator`'s authenticated JSON admin surface. One context owning
   both audiences is the thing to avoid. The decisive point there was that the
   ArchUnit fence is the *feature*: `storefront` cannot import `touroperator`,
   which forces the read seam.
2. **Shopify OS 2.0 scope.** Ground in fresh research before any theme work
   (decision from the rebuild conversation). It was parked as downstream of the
   render path — **that block is gone**: the render path is decided (Mustache,
   in-process). So this is no longer blocked, it is simply not started, and it is
   the first thing to settle when theme work begins. What it has to answer is
   bounded by what Mustache can express — logic-less templates, and no custom
   tags, which forces a section's schema out of its template. **That constraint,
   and what is still open under it, is `backend/PATTERNS.md` §2b.**

3. **Where a platform-level security event goes.** A refresh-token *reuse* — a
   replayed refresh token, i.e. a probable theft — is detected in
   `RefreshAccessTokenUseCase`, revokes the whole token family
   (`revokeAllByFamilyId`), answers 401, and is reported **only** through
   `diagnosticLog.warn(...)` → SLF4J. Verified 2026-08-11: no `AuditTrailPort` in
   that class, and nothing in `src/main` calls `AuditActor.system()`.

   **This sat in Debt until 2026-08-11 and did not belong there.** Nothing is
   owed: detection works, revocation works, the attacker is locked out. The
   security *control* is complete. What is missing is the ability to answer a
   question afterwards — "was anyone's session stolen last month?" — and there is
   no asker, because nothing is deployed. It also has no action behind it, only a
   question, which is the tell: every real Debt entry names a thing to do.

   **The tenant audit trail cannot take it as it stands.**
   `audit_log.tour_operator_id` is NOT NULL and `NewAuditEntry` requires it; this
   event happens in `identity` and has **no** tour operator (a user may belong to
   zero, one or several). The surface is also tenant-scoped by construction:
   `ListAuditLogUseCase.SCHEMA` is `.tenantScoped()`, and the controller sits under
   `/api/tour-operators/{id}/audit-log` behind the membership gate. So an invented
   tenant would file a platform security event in one customer's activity feed,
   readable by their staff and invisible to us.

   Note the rule does **not** force this: §8b audits every *operator-facing*
   mutation, and revoking a token family is not one.

   **Not forced until there is a deployment with real users** — which is also
   when the cheapest answer might turn out to be "the host already retains and
   searches the app log," costing no code at all. The alternative, if it is not,
   is a non-tenant home for security events; **what must not happen is making
   `tour_operator_id` nullable because a backlog list said to.**

4. **The cart's cookie has to join the CSRF posture, or depart from it on
   purpose.** (2026-08-22) — nothing is owed; the point is that the answer must
   exist before the first cart write ships, not after.

   **The posture today is coherent, and it is not the one it looks like.**
   `SecurityConfig` calls `csrf(AbstractHttpConfigurer::disable)`, which reads as
   "no CSRF defence" — but a cookie-authenticated write already exists.
   `POST /api/auth/refresh` is a **public** route that reads the refresh token
   from a `@CookieValue`, and a browser attaches cookies to cross-site requests.
   (`/logout` reads the same cookie but is *not* public, so a bearer token gates
   it — the distinction is the whole point, and it is one route not two.) What
   defends refresh is the cookie, not the framework:
   `same-site: Strict` (application.yml, default), plus `httpOnly`, `secure`, and
   a path scoped to `/api/auth`. **Disabled CSRF and `SameSite=Strict` are a
   deliberate pair, not an omission** — which is worth writing down, because the
   next person to read `csrf().disable()` will otherwise conclude the opposite.

   So the cart question is narrower than "does the storefront need CSRF": **can a
   cart cookie be `SameSite=Strict` too?** For a theme's own JavaScript posting to
   its own storefront host, yes. Two things to check before assuming it:

   - **A return from a third party is not same-site.** A payment provider
     redirecting back to the storefront will not carry a `Strict` cookie on that
     first request, which is the classic way a `Strict` session appears to vanish
     at exactly the wrong moment. `Lax` fixes the redirect and weakens the
     defence; a checkout that keeps its own token does not need the cookie on that
     hop at all. Decide it with checkout, not before — but know it is coming.
   - **The storefront is a different registrable domain from the admin** in the
     candidate mapping (`vointika.com` versus `{handle}.myvointika.com`,
     `PATTERNS.md` §2b), and that split exists for cookie isolation. Whatever the
     cart cookie's attributes are, they are about the storefront's domain, not the
     admin's.
   - **`Strict` on the refresh cookie is already a deployment constraint, today.**
     A `Strict` cookie is not sent on a cross-**site** fetch either, so the admin
     SPA and the API must stay on one registrable domain or
     `POST /api/auth/refresh` silently stops working — no error to read, just a
     session that will not renew. Same *origin* is not the requirement and is not
     the case: CORS allows `localhost:3000` against an API on `:8080`, which is a
     different origin and the same site, so `Strict` is unaffected. Splitting them
     across registrable domains is what would break it.

     The mechanism has a second dependency worth naming, because it reads as
     background until it is missing: the cookie only rides that cross-origin fetch
     at all because `CorsConfig` sets `setAllowCredentials(true)` — Spring's
     default is `false` and no config key turns it on. Without it the cookie would
     not be sent whatever `SameSite` said, and the paragraph above would be right
     about the conclusion and wrong about the reason.

     That is the other half of the fact above: **`Lax` on the storefront buys
     inbound navigation and pays in defence; `Strict` on `/api/auth` buys defence
     and pays in topology.** Both prices come due in §2b, which is where the
     domains get chosen — so choose them knowing the cookies already have
     opinions.

   Two smaller things ride along and are worth deciding at the same time:

   - **A cart cookie is a third cookie concept**, and the storefront's own
     unlock cookie has already answered most of this question — in the other
     direction. `PasswordPageController.unlockCookie` is `httpOnly(true)`,
     `secure(request.isSecure())`, **`sameSite("Lax")`** and `path("/")`. So the
     storefront domain does not run "csrf disabled + `Strict`"; it runs "csrf
     disabled + `Lax`", and a cart joining "the posture" has to say which posture.

     **`Lax` there is the same trade this entry files under checkout, already
     taken.** A `Strict` cookie is not sent on a top-level cross-site navigation,
     so a visitor following an inbound link into a store they had unlocked would
     be shown the gate again. That is the payment-redirect problem in a cheaper
     costume, and it was settled in code before it was written down anywhere —
     `unlockCookie`'s javadoc gives reasons for `secure` and for the session
     lifetime and **none for `Lax`**, which is why this reads as an open question
     rather than a precedent.

     The other two attributes differ from the refresh cookie as well, and a
     convention has to reconcile them rather than pick a third variant:
     `secure(request.isSecure())` against a configured `secure`, and `path("/")`
     against `/api/auth`. A cart cookie scoped to `/cart` would be the third
     answer to a question already answered twice — which is the argument for a
     convention, not against one.
   - **§8b does not cover it.** "Every operator-facing mutation appends to the
     audit trail" is about the operator's own actions; a visitor adding to a cart
     is not one. Saying so explicitly is what stops someone filing visitor traffic
     into a tenant's activity feed — and it re-opens the `STOREFRONT` actor
     question `contact` already parked once.
