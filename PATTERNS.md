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
- `presentation/response/FooResponse` — a record carrying a `type` discriminator
  (`"currencies"`).
- `db/migration/<ctx>/V?__*.sql` — seeds the curated launch set.

**Nested-only variant:** a reference type used *only* inside another response
(e.g. `Country` nested in a timezone) keeps entity + JpaEntity + Mapper +
Response and **drops** the repository / use case / controller. Don't add a
standalone endpoint until something needs it.

Canonical: `reference` — `Timezone`/`Currency` full, `Country` nested-only.

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
config key (+ any assets like a template file); zero code, zero migration.
Canonical: `app.identity.ui-languages` + `GET /api/ui-languages`.

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

- Copied `*ControllerDocumentationTest` files carry a stray
  `@MockitoBean TourOperatorMembershipCheck` — strip the import + field until the
  touroperator context provides it.
- Boot 4 autoconfiguration is per-starter: depend on the **Boot starter**
  (`spring-boot-starter-kafka`), not the raw library (`STACK.md` §gotchas).
- The autoconfigured `KafkaTemplate` is typed `<?, ?>` — inject the **raw**
  `KafkaTemplate`, not `<String, Object>`.
- Multi-line email templates end `</body>\n\n</html>` — assert `endsWith("</html>")`,
  not `</body></html>`.
