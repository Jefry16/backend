# Slice — delete the storefront serving side

Executable from a cold start. Read LAW (`../CONSTITUTION.md`), `../MAP.md`,
`CLAUDE.md`, `PATTERNS.md`, `STACK.md` first, then this.

> **Run this slice.** Branch `chore/drop-storefront-serving`. Baseline the suite
> (`rm -rf target && ./mvnw -o test`) and record the number before touching
> anything. Expect it to drop a lot; that is the point.

## Why

The render path is being restarted from nothing (MAP open decision 6). Everything
that exists to *serve* a storefront was built for an architecture — a separate
renderer calling in over HTTP with a shared secret — that is no longer assumed.
Keeping it means the next design is shaped by code written for the old one.

**This deletes the serving side only.** Operator-authored *data* stays. The
distinction is the whole slice: an admin endpoint that lets an operator write
content stays; anything whose only job is to hand that content to a renderer goes.

## Delete

**The `rendering` context, entirely** — 26 files under
`src/main/java/com/vointika/rendering` and `src/test/.../rendering`. Controllers,
use cases, `TenantResolver`, `LocaleResolver`, `NavigationAssembler`,
`SeoResolver`, every DTO and response record, `RenderingUseCaseConfig`,
`RenderingPublicRoutes`.

**All four storefront seam ports, with their views, impls and tests** — ~20 files
named `Storefront*`. Verified: each is used only by `rendering` plus its own
implementing context, so nothing else breaks.

- `shared/port/StorefrontOperatorQuery` + `StorefrontOperatorView` +
  `StorefrontOperatorTranslationView` → impl in `touroperator`
- `shared/port/StorefrontExperienceQuery` → impl in `experience`
- `shared/port/StorefrontPageQuery` → impl in `page`
- `shared/port/StorefrontNavigationQuery` → impl in `touroperator`

**The shared-secret machinery** —
`shared/web/security/StorefrontApiSecretFilter`, `StorefrontApiProperties`, their
tests (including the header-name pin), `app.storefront.shared-secret` in
`application.yml`, and `APP_STOREFRONT_SHARED_SECRET` in `docker-compose.yml`.
The filter exists solely to authenticate a caller that no longer exists.

**The ArchUnit rule `rendering_depends_only_on_shared`** — it fences a package
that will not exist. Delete it; the generic `contexts_do_not_depend_on_each_other`
rule already fences whatever replaces it, derived from the package structure.

**Docs** — PATTERNS §8c ("BFF endpoints"), the "The BFF API" bullet in
`CLAUDE.md`'s API-surfaces list, and any STACK/CONTEXT-AUDIT reference to the
internal API. PATTERNS §4d's parked-read-half note needs rewording, not deleting
(see below).

## Keep — and this is the part to get right

**Every migration.** They hold operator data, not serving code. Nothing is
dropped, no new migration is written.

**All admin-authored content and its endpoints**: page and experience content and
their handles, per-locale translations, operator translations, SEO fields on
operator/experience/page, navigation menus and their item trees, the storefront
password and its `GET`/`PUT` settings endpoints, `experiences.starting_price`.
An operator can still author everything; nothing serves it yet.

**The PATTERNS §4d write guards** in `UpsertExperienceTranslationUseCase`,
`CreateExperienceUseCase` and the `page` equivalents. Their read path is being
deleted, so nothing can observe a shadowing handle — but one could still be
**stored**, and it would surface as an unreachable page the day a read path
returns. A defect committed now and found much later is worse than a guard with
no current motivation. Update §4d's note to say the read half is gone entirely
rather than "parked".

**`PublicRouteRegistrar`, `PublicRoute`, `DocsPublicRoutes`** — used by auth and
invitations, nothing to do with the storefront.

## Consequences to state, not discover

- **The storefront password becomes write-only.** An operator can set it; nothing
  verifies it, because `VerifyStorefrontPasswordUseCase` and the port method go.
  Fine — it is a setting waiting for a gate.
- **Every remaining endpoint is the JWT-authenticated admin API.** There is no
  `/api/storefront/**` surface at all, and no shared secret anywhere.
- **Two unwired columns become three-ish**: `starting_price`, experience SEO, and
  now operator SEO and translations have no reader either. All are authored
  through real admin endpoints, so they are dormant, not dead. Say so in MAP.

## Verification

1. `rm -rf target && ./mvnw -o test` — green, with the drop explained by the
   deleted tests, not by anything breaking.
2. **After compiling, sweep for what the deletion orphaned.** This is where the
   real work is: JPA repository methods, mapper branches and domain accessors that
   only the deleted query impls called. Use the `CONTEXT-AUDIT.md` §2 method and
   *print the examined counts*. The last two deletions each left orphans that only
   a method-level pass found — a port can be live while a method under it is not.
3. `./mvnw -o test -Dtest=ArchitectureTest` — expect 8 rules, not 9.
4. Confirm no `/api/storefront` route remains:
   `grep -rn "api/storefront" src/` → nothing.

## Landing

Update `MAP.md`: the `rendering` row goes (or becomes a tombstone naming what was
deleted and why), the `touroperator`/`experience`/`page` rows lose their seam-port
sentences, and a ledger entry records the deletion with the serving-vs-authored
distinction. Open decision 6 stays exactly as it is — it is the reason for this
slice, and the research in it is what the next design starts from.

Do **not** delete `~/storefront-archive.bundle`.
