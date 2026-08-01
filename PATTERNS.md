# Vointika Backend — Patterns

The **how we build** reference: the concrete shapes that have repeated across
slices. Read this at LAW §5.2 ("look before you build") — a new slice follows
the matching recipe here instead of reverse-engineering existing code or copying
the archive.

Companion to `CONSTITUTION.md` (the *rules*) and `STACK.md` (the *versions*).
This file is *recipes*, not law.

**How this stays lean:** a pattern earns a spot only when it's **repeated ≥2×
and stable**. Deviate deliberately (LAW §2.3 — structure follows need), and when
you do, either it's a one-off (don't touch this file) or it's the new pattern
(update the recipe). Prune anything that stops being true.

---

## 1. Context shapes

Every bounded context is one of two shapes. The layer DAG
(`domain ← application ← {infrastructure, presentation}`) and cross-context
isolation are ArchUnit-enforced; `domain` stays pure (no Spring/JPA/Jackson).

- **Full context** — owns entities and an HTTP surface. Layers:
  `domain / application / infrastructure / presentation`.
  Canonical: `identity`, `reference`.
- **Worker module** — reacts to events, owns no entities and no HTTP. Layers:
  `application / infrastructure` only (no `domain`, no `presentation`).
  Canonical: `notification`.

`shared` and `reference` are shared kernels — importable by any context.
Everything else is isolated: a context reaches another only via a shared query
port or an event (never a direct import).

## 2. What goes in each layer

- `domain/entity` — aggregates, pure, behavior-rich (mutators bump `updatedAt`).
  `domain/valueobject` — records that validate in their constructor
  (`Email`, `Password`). `domain/enums`. `domain/repository` — the outbound
  repository *interfaces* the use cases depend on.
- `application/usecase` — one class per operation, a **plain constructor-injected
  POJO** (no Spring annotations — keeps the layer framework-free).
  `application/dto/input` + `dto/output`. `application/port` — outbound port
  interfaces the use case needs (impl lives in `infrastructure`).
- `infrastructure/config` — the context's `UseCaseConfig` (`@Bean`-wires every
  use case by hand) + its `@ConfigurationProperties` records.
  `infrastructure/persistence/{entity,mapper,repository}`. `infrastructure/port`
  — adapter impls of application/shared ports. `infrastructure/query` — impls of
  shared query ports this context provides. `infrastructure/security`,
  `infrastructure/consumer` (workers).
- `presentation/{controller,request,response}`.

## 3. Persistence per aggregate — the 6-file recipe

For an aggregate `Foo`:

1. `domain/entity/Foo` — pure domain entity.
2. `domain/repository/FooRepository` — the interface (what the use case sees).
3. `infrastructure/persistence/entity/FooJpaEntity` —
   `@Entity @Table(schema="<ctx>", name="foos")`, Lombok
   `@Getter @NoArgsConstructor @AllArgsConstructor`.
4. `infrastructure/persistence/mapper/FooMapper` — static `toDomain(jpa)`
   (+ `toJpa` when there are writes).
5. `infrastructure/persistence/repository/FooJpaRepository` — Spring Data
   `extends JpaRepository<FooJpaEntity, UUID>`.
6. `infrastructure/persistence/repository/FooRepositoryImpl` —
   `@Repository implements FooRepository`, delegates to the JpaRepository + Mapper.

Canonical: identity `User*`, reference `Timezone*`.

## 4. Reference-data slice (read-only lookup)

A curated, read-mostly table (countries, timezones, currencies). = the
persistence recipe (§3) **plus**:

- `application/usecase/ListFoosUseCase` — returns `repository.findAll()`.
- `presentation/controller/FooController` — `GET /api/foos`, **authenticated**
  (no public route), maps domain → `FooResponse`.
- `presentation/response/FooResponse` — a record following the response identity
  convention below (§4a).
- `db/migration/<ctx>/V?__*.sql` — seeds the curated launch set.

**Nested-only variant:** a reference type used *only* inside another response
(e.g. `Country` nested in a timezone) keeps entity + JpaEntity + Mapper +
Response and **drops** the repository / use case / controller. Don't add a
standalone endpoint until something needs it.

Canonical: `reference` — `Timezone`/`Currency` full, `Country` nested-only.

## 4a. Response identity — `id` + `context` (HOUSE RULE)

Every resource-representing response record identifies itself with exactly two
meta fields:

- **`id`** — the entity's id. **Never** a prefixed name (`userId`, `operatorId`,
  `tourOperatorId`, …). Just `id`, always.
- **`context`** — a string naming the collection the entity belongs to
  (`"users"`, `"currencies"`, `"timezones"`). Set via the two-constructor pattern
  so callers never pass it. (This is the discriminator; it is **not** called `type`.)

The `context` is the entity's *own* collection: a team-member row is a user with a
role, so it is `id` = the user's id + `context: "users"` (not `"members"`).
Action-result responses that aren't a resource (e.g. `LoginUserResponse`,
`SetAvatarResponse`) carry neither field.

Canonical: `CurrencyResponse` (`id`, `context:"currencies"`), `MemberResponse`
(`id`, `context:"users"`).

## 4b. Paginated list endpoints (cursor + filter + sort)

Any list over **tenant or growable data** (members, bookings, orders, audit) MUST
use the shared list framework — **never an unbounded array** (the roster shipped
that way once and it was the recorded mistake this fixes). The recipe:

1. **Schema** — a `public static final ListSchema SCHEMA` on the use case:
   `.tenantScoped()` (scopes to the entity's `tourOperatorId`), `.set/text/number/
   instant(...)` for each filterable field, `.sortable(...)` + `.defaultSort(...)`.
2. **Repository** — `CursorPage<Foo> list(ListQuery query)`, delegating to the
   shared `CriteriaListExecutor.list(FooJpaEntity.class, SCHEMA, query, Mapper::toDomain)`.
   The executor does keyset cursor pagination (page size 20, tie-broken on `id`),
   the filter predicates, and the sort.
3. **Use case** — `execute(ListQuery, callerId)`: gate (e.g. `ensureMember`), call
   `repository.list`, enrich the page's rows (batched, no N+1), return the
   `CursorPage` with its `nextCursor` unchanged.
4. **Controller** — inject `ListQueryParser`, `parse(request, SCHEMA, tenantId)` →
   `ListQuery`, return `CursorPageResponse.of(page, FooResponse::from)` →
   `{ "data": [...], "nextCursor": "..." }`.

Query shape: `?filter[role][in]=OWNER,ADMIN&sort=-joinedAt&cursor=…`. The cursor is
opaque (base64, keyset on sort-field + id); `nextCursor` is null on the last page.
Canonical: `ListMembersUseCase` + `GET /api/tour-operators/{id}/members`.

## 4d. Two namespaces read as one must be validated as one

A storefront handle resolves against **localized handles first, canonical handles
second** (`StorefrontPageQuery`, `StorefrontExperienceQuery`). That makes them one
namespace on the read side, so uniqueness has to be checked across both on every
write — otherwise one silently shadows the other and the shadowed page becomes
unreachable in that locale, with no error at any point.

`page` shipped with each namespace checked only against itself, which is the natural
mistake: the create/rename path asks `pages`, the translation path asks
`page_translations`, and each looks complete on its own. The three write paths now
cross-check:

- **create / rename a canonical handle** → also reject it if any *other* page uses it
  as a localized handle in **any** locale;
- **upsert an explicit localized handle** → also reject another page's canonical handle;
- **derive a localized handle** → probe *both* namespaces, so the auto-suffix never
  lands on one either.

Matching the page's **own** canonical handle is fine — it resolves to the same page.
The general rule: when a read path consults two sources in precedence order, list the
write paths that feed each and make every one of them check both.

`experience` had the same defect and now has the same guards, with one difference worth
knowing before you read the two side by side and think one is wrong. **Whether a
cross-namespace collision is a 409 or a suffix depends on who chose the value, not on
which namespace it came from.** A page handle is operator-chosen and permanent, so a
clash is a 409 the operator can act on. An experience's canonical slug is *derived from
its name*, so its create path widens the probe instead — the auto-suffix simply steps
over localized slugs too, and the operator sees a `-2` rather than a 409 for a value
they never typed and have no field to correct. The explicit localized slug is
operator-chosen in both, and 409s in both.

One consequence: the any-locale probe needs an exclusion parameter only where a *rename*
path calls it (page). Where the canonical value is immutable (experience), create is the
only caller and never excludes — so the parameter, and page's nil-UUID sentinel standing
in for "exclude nothing", are both absent by LAW §2.4.

**These guards are pre-checks, not constraints, and that is the one thing this recipe
cannot fix.** Uniqueness *within* a namespace is backed by a unique index, so a lost race
surfaces as a duplicate-key failure and the loser is rejected. There is no index spanning
the two tables and there cannot be one without a trigger — so two concurrent writes, one
per namespace, can still land on the same value and produce exactly the shadowing the
guards exist to prevent. The window is small and both `page` and `experience` carry it.
Treat the cross-namespace check as closing the reachable-by-one-request hole, not as
making the invariant true.

## 5. Read-time URL resolution (never store URLs)

Store a bucket-relative **storage key** on the row; resolve it to an absolute
URL **at read time** in the controller via `shared.media.MediaUrlResolver`. The
response exposes `<thing>Url`. Changing the bucket/domain then needs no data
migration.

- Key shapes: `users/{userId}/{uuid}-avatar.{ext}`, `flags/{iso2}.svg`.
- Canonical: identity avatar, reference country flag.

## 6. Cross-context communication

A context never imports another's types. Two channels only:

- **Shared query port** (synchronous read) — `shared.port.<Noun>Query` +
  a `<Noun>View` record. The owning context implements it in
  `infrastructure/query`; the caller depends on the interface. Views carry
  primitives, not another context's enums.
  Canonical: `UserAccountQuery`, `UserTourOperatorMembershipsQuery`.
- **Event over Kafka** (asynchronous) — see §7.

## 7. Event flow (Kafka)

- **Publish:** a use case calls `EventPublisherPort.publish(new FooEvent(...))`.
  Event records live in `shared/event`. The producer adapter routes by class →
  topic (`shared.infrastructure.kafka`); topic name = `<producing-context>.<event-kebab>`,
  registered in `EventTopics`, keyed by recipient (e.g. email).
- **Consume:** `@KafkaListener` in the consuming context's
  `infrastructure/consumer`; fire-and-forget consumers log-and-swallow so one bad
  record never stalls the partition.
- **Request context rides the event/headers:** the consumer can't query the
  producer's DB, so carry what it needs on the event (e.g. `locale`) or a Kafka
  header (correlation id).
- Canonical: identity events → notification consumers.

> **Fire-and-forget is non-critical-only.** This shape suits *drop-tolerant*
> notifications — recoverable by the user, and the SES adapter already retries
> transient failures. A **must-not-drop** event (payment, refund, booking state)
> must **not** log-and-swallow: it needs a different shape — at-least-once +
> idempotent consumer + retry/DLQ — whose decided direction is in the **MAP
> backlog** ("Critical-event delivery"). Build that shape when the first such event
> lands, not before (§2.3).

## 8. Config-driven capability (grow by config, not code)

A capability whose *set* grows over time (UI languages, email locales) is a
config allowlist (`@ConfigurationProperties`), validated in the use case, and
exposed via a read endpoint when the frontend needs the list. Growing it = add a
config key (+ any assets like a template file); no migration, and no code
*for the capability itself*.
Canonical: `app.identity.ui-languages` + `GET /api/ui-languages`.

**Count the consumers of the allowlist before calling it config-only.** Adding a
UI language is a yml edit for the picker and for validation — but transactional
email keeps its own list (`ClasspathTemplateCatalog.LOCALES`) plus a template pair
per (type, locale), and a language missing from it does not fail: the send falls
back to English, so the user silently gets the wrong language. A second list that
must agree with the allowlist needs a test that fails the build when they diverge
(`TemplateLocalesTrackUiLanguagesTest`), not a comment saying it should track.

## 8a. Rate limiting (three layers)

One primitive — `RateLimiterPort.tryAcquire(key, limit, window)` (fixed-window,
**fail-open**, Redis-backed). Keys are `rl:{dimension}:…`; callers own their
namespace. Pick the layer by what you're defending:

- **A — per-IP endpoint rules** (pre-handler, `EndpointRateLimitFilter`): abuse
  defense on public/unauthenticated routes. Declare per-context via the
  `RateLimitRuleRegistrar` SPI (mirrors `PublicRouteRegistrar`) — a
  `RateLimitRule(method, pathPattern, limit, window)`. Matched by Spring
  `PathPattern`, keyed **`rl:ip:{METHOD}:{pattern}:{ip}`**. Canonical:
  `IdentityRateLimitRoutes`, `TourOperatorRateLimitRoutes`.
- **B — per-identity in-use-case throttles**: input-dependent keys the filter
  can't see (email, account). Inject `RateLimiterPort`, call
  `tryAcquire("rl:{action}:{scope}:{value}", n, window)`. Canonical:
  `RegisterUserUseCase` (`rl:register:email:{email}`).
- **C — blanket per-user cap** (`ApiRateLimitFilter`, per authenticated user): a
  runaway-script backstop, automatic on the authed API. Nothing to add.

**The keying gotcha:** layer A keys on the matched **pattern**, never the concrete
URI — else a path-variable route (`/api/invitations/*/accept`) buckets per token
value and never limits. Rules live with the context that owns the endpoint (SPI),
never in a central hardcoded map.

## 8b. Audit append (every operator-facing mutation)

A use case that mutates an operator-facing entity records the action through
`shared.port.AuditTrailPort.append(NewAuditEntry)` — **inside the same
`TransactionRunner` block as the mutation**, so the entry commits and rolls
back atomically with the action ("no unaudited mutation"; a failed append fails
the action). Exception: a mutation whose target is object storage (S3) appends
in its own transaction AFTER the successful write — storage can't roll back, so
that is the honest best-effort. Actor name is frozen at write (filter-only,
never sortable — it's nullable and keyset cursors need non-null sort keys).
Canonical: any experience/audience mutating use case; the port impl lives in
`audit/infrastructure/integration`.

## 8c. Internal (BFF) endpoints — `/api/internal/**`

The public storefront never talks to this API: its BFF does, server-to-server.
That surface is authenticated by a **shared secret**, not a JWT. Page reads live
in `rendering`; an internal endpoint that *mutates* belongs to the context owning
the data (a cart write is cart's, not rendering's) and brings its own registrar.
Adding one:

1. Map it under `/api/internal/…` and take the tenant as a **slug** path
   variable — the storefront knows tenants by subdomain, not by id.
2. Register the exact pattern in `RenderingPublicRoutes` (a
   `PublicRouteRegistrar`). This does **not** make it public: it only stops
   `anyRequest().authenticated()` from demanding a JWT. `InternalApiSecretFilter`
   still 401s any call whose `X-Internal-Secret` doesn't match, before any
   handler runs. Forgetting the registrar → 401 even with the right secret;
   registering a pattern that doesn't exist → nothing (fail-closed both ways).
3. Compose the **whole page** in one response. One internal call per page render
   is the contract — never make the BFF fan out (§6 applies: `rendering` reaches
   every other context through shared ports and imports none of them, enforced by
   ArchUnit).
4. Anything a visitor guesses at (a password, a token) answers **200 with a
   boolean**, not 401 — a 401 on this surface means "bad shared secret" and
   nothing else. Unknown tenant and wrong answer must be indistinguishable.
5. In a `@WebMvcTest`, `@Import` the registrar alongside `SecurityConfig` and pin
   `app.internal.shared-secret` via `properties = …` (it has no default). Without
   the registrar every assertion passes vacuously as 401.

## 8d. Losing a race against a unique constraint

A write that passes its `existsBy…` pre-check can still lose to a concurrent one:
the DB constraint decides. JPA flushes at **commit**, so the failure surfaces from
`transactionRunner.run(...)`, never from `repository.save(...)` — translating in a
repository implementation would miss it.

`SpringTransactionRunner` is the one translator. It converts Spring's
`DuplicateKeyException` into `shared.exception.UniqueConstraintViolationException`;
a use case catches that and answers in its own terms:

```java
try {
    transactionRunner.run(() -> { repository.save(x); ... });
} catch (UniqueConstraintViolationException e) {
    throw new ResourceAlreadyExistsException("A page with that handle already exists");
}
```

Two things this deliberately does **not** do. It does not translate the parent
`DataIntegrityViolationException` — foreign-key, not-null and check-constraint
failures are defects, not races, and must stay 500s. And it does not let the
framework type reach the application layer; ArchUnit fails the build if it does.

**So a use case that must answer for a NON-unique constraint has to ask, not catch.**
Postgres raises 23505 for a unique violation (→ `DuplicateKeyException`, translated)
but 23503/23502/23514 for foreign-key/not-null/check (→ the untranslated parent).
`DeleteMetaobjectDefinitionUseCase` caught `UniqueConstraintViolationException` to
turn "a reference metafield still pins this type" into a 409; the catch could never
fire and the delete 500'd. The fix is an `existsBy…` pre-check before the write —
which leaves the concurrent-creation race as a 500, and that is the intended outcome
for an unexpected constraint failure. `SpringTransactionRunnerTranslationTest` pins
the boundary.

Uncaught, the exception maps to 409.

The same move works for any library a use case reaches for: `MetafieldValueValidator`
needed a JSON parser, so it asks `JsonSyntaxPort.isWellFormed(value)` and the Jackson
call lives in `infrastructure/port`. The application layer has no exemptions — a
library in a use case means a port is missing.

**Logging follows the same rule.** If a *side effect* fails and the caller does not
care — deleting an object whose row is already gone, enqueuing a welcome email — the
adapter swallows and logs it, and the port documents that it never throws. Only when
the use case itself has something to report (a security signal, a branch taken because
config was missing) does it reach for `DiagnosticLogPort`, which takes the calling
class so log names still point at the reporter.

## 4c. One DTO or two at the application boundary

A use case takes an `Input` from `application/dto/input` and the controller owns a
`Request` in `presentation/request`. Keep both **only when they differ**. In identity
nine pairs do — the `Input` carries a `userId` from the authenticated principal, or a
`language` the body never had — and four were byte-identical copies, since deleted.

An identical copy is not a seam. Add a field to the request that the use case needs
and both change in lockstep, so it insulates nothing while costing a file and a
mapping step.

**Check the nested records separately from the wrapper.** `ReplaceMenuItemsRequest`
and `ReplaceMenuItemsInput` genuinely differ — the input adds the caller and the two
path ids — but the tree node inside them was identical, so the controller carried a
recursive copy that ran on every save. A pair can be a real seam at the top and a
pure copy one level down; the nested type is where the cost is, because collapsing it
deletes a mapper and not just a file.

When you collapse one, **the application record is the survivor** and the controller
binds to it:

```java
public ResponseEntity<LoginUserResponse> login(@RequestBody LoginUserInput input) {
    var output = loginUserUseCase.execute(input);
```

Never the other way. A use case referencing a `presentation` type inverts the layer
graph and ArchUnit fails the build.

The condition, and the build enforces it: the surviving record must carry **no
annotations**. The application layer's allowlist is `com.vointika..` + `java..`, so a
`@JsonProperty` or a Jakarta validation annotation on it is a compile-time-legal but
build-breaking change — and the correct answer at that point is to reintroduce a
presentation DTO, because the shapes have genuinely diverged.

Responses are the mirror image: `LoginUserOutput` carries `accessToken` *and*
`refreshToken`, `LoginUserResponse` carries only the access token because the refresh
token leaves in an httpOnly cookie. That pair stays.

## 9. Testing shapes

- **Unit** — JUnit5 + Mockito, no Spring: every value object, entity behavior,
  and use case (inline `TransactionRunner` lambda for the tx). Carry the
  security-critical cases (anti-enumeration, timing parity, token rotation).
- **Controller documentation test** — `@WebMvcTest(FooController.class)` +
  `@Import(SecurityConfig.class)` (add `IdentityPublicRoutes` when testing a
  public route), collaborators `@MockitoBean`, assertions + RestDocs
  `document(...)`. Authenticated endpoints send `Authorization: Bearer …` and
  stub `AccessTokenValidatorPort.isValid/extractUserId`.
- **ArchUnit** — when a context lands, add its per-context isolation rule (and a
  client fence for any confined library, mirroring the Redis/Kafka fences).

## 10. Migrations

Per-context folder `db/migration/<ctx>/`, independent V-sequence, own Postgres
schema (`FlywayPerDomainConfig`). **Never modify an applied migration** — add the
next `V`. Curated reference/seed data lives in the migration.

## 11. Recurring gotchas (check before you trip)

- Boot 4 autoconfiguration is per-starter: depend on the **Boot starter**
  (`spring-boot-starter-kafka`), not the raw library (`STACK.md` §gotchas).
- The autoconfigured `KafkaTemplate` is typed `<?, ?>` — inject the **raw**
  `KafkaTemplate`, not `<String, Object>`.
- Multi-line email templates end `</body>\n\n</html>` — assert `endsWith("</html>")`,
  not `</body></html>`.
- **Case-fold with `Locale.ROOT`, never the JVM default.** `"IT".toLowerCase()`
  under a Turkish default locale is `"ıt"` (dotless), so locale codes, slugs and
  handles silently stop matching depending on which machine served the request.
  `LocaleCode` has always done this; `LocaleResolver` had to be fixed to.
- An in-tx `save(entity)` followed by a bulk `@Modifying` JPQL on a **different**
  table needs `@Modifying(clearAutomatically = true, flushAutomatically = true)`.
  Without `flushAutomatically`, Hibernate skips the auto-flush (no query-space
  overlap) and the clear silently **discards the pending save**. Both snapshot
  propagators carry load-bearing comments on this — don't strip them.
