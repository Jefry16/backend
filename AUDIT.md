# Context audit — `storefront` — 2026-08-14

> **Status: all three findings fixed in PR #155** (2026-08-14), plus the
> locked-state coverage gap. §3.1 was fixed by *removing* the duplication rather
> than testing it: both registries now derive from `StorefrontRoutes.PAGE_ROUTES`.

Investigation only; no code changed. Method is `CONTEXT-AUDIT.md`, run against one
bounded context as that playbook intends.

**Why this context.** The repo-wide pass earlier today was mechanical — port
liveness, DTO pairs, deps, config keys, catch blocks. It never read a context end to
end. `CONTEXT-AUDIT.md` lists the contexts already given a proper pass — `identity`,
`touroperator`, `experience`, `metafield`, `shared`, `audience`, `audit` — and
`storefront` is not among them, despite being rebuilt from nothing across eleven PRs
today (#137–#152).

Suite at time of audit: **1192 green**, HEAD `44b61d7`.

**Three findings. Two are unenforced must-agree sets, and both fail silently in the
direction of leaking or losing a security control.**

---

## §1 · Measure

| context | files | LOC | endpoints | files/endpoint | LOC/file |
|---|---|---|---|---|---|
| **storefront** | **32** | **2301** | **11** | **2.9** | **71** |
| page | 45 | 2305 | 12 | 3.7 | 51 |
| pickup | 17 | 790 | 5 | 3.4 | 46 |
| media | 24 | 1340 | 5 | 4.8 | 55 |
| contact | 17 | 606 | 3 | 5.6 | 35 |
| reference | 37 | 817 | 4 | 9.2 | 22 |

`storefront` is the **leanest per endpoint and the densest per file** of any context
with endpoints. That shape is explained, not suspicious: it owns no entities, so it
has no `domain` package and none of the six-file persistence recipe — it is policies,
use cases and response assembly only. The density is Javadoc on those policies (§6).

Verified by: `find`/`grep -c` per context, run in this session.

---

## §2 · Dead code

**None found.**

| scan | examined | dead |
|---|---|---|
| classes (Spring-annotated excluded) | 32 | 0 |
| public methods, dotted call sites | 59 | 0 genuine (36 flagged) |
| application-layer imports outside the allowlist | all | 0 |

The 36 flagged methods are the false positives `CONTEXT-AUDIT.md` §2 enumerates, and
they break down exactly: **20** are `record` declarations my regex reads as methods,
**7** are `@Bean` factories in `StorefrontUseCaseConfig`, **7** are MVC handler
methods reached by dispatch, and **2** are framework overrides (`preHandle`,
`addInterceptors`). None is reachable by a dotted call and all are live.

Verified by: class loop over 32 files excluding
`@Component|@Configuration|@RestController|@Repository|@Service`, counting
`grep -rl "\bClass\b"` outside its own file; method loop counting
`\.name\(|::name` across `src/main` and `src/test`; and
`grep -rhoE "^import ..." src/main/java/com/vointika/storefront/application | grep -vE "^(com\.vointika|java\.)"`
→ empty.

### The cutback left nothing stale

`storefront` was cut to a placeholder (#135) and rebuilt, so the playbook's rule
applies: audit the removal, and grep the removed feature's nouns.

| deleted noun | hits | verdict |
|---|---|---|
| `ThemeContextDump` | 0 | gone |
| `StorefrontShopQuery` | 0 | gone (renamed #147) |
| `LocaleResolver` | 0 | gone |
| `PolicySlug` | 1 | **accurate history**, not a stale claim |
| `StorefrontOperatorQuery` | 1 | **accurate history**, not a stale claim |

Both surviving hits are prose that correctly describes the past:
`PolicyController.java:47` explains where its slug mapping came from, and
`ArchitectureTest.java:81` explains why a rule exists. Neither asserts that deleted
code is present. **Read both; no correction needed.**

Verified by: `grep -rl` per noun across `src/main` and `src/test`, then reading each
surviving hit.

---

## §3 · Findings

### 3.1 Four registries must agree on every page route, and nothing enforces it

**`src/main/java/com/vointika/storefront/infrastructure/web/StorefrontWebConfig.java:24`** ·
**high**

A storefront page route must be registered in four places, which `CLAUDE.md` already
states: the `@GetMapping`, the `PublicRoute` GET entry, the `PublicRoute` HEAD entry,
and the lock interceptor's `addPathPatterns`. **All four agree today** — I checked all
eight addresses in each. Nothing keeps them agreeing.

The interceptor is an explicit allowlist:

```java
.addPathPatterns(StorefrontRoutes.HOME, StorefrontRoutes.LOCALE,
        StorefrontRoutes.EXPERIENCES, StorefrontRoutes.LOCALIZED_EXPERIENCES,
        StorefrontRoutes.POLICY, StorefrontRoutes.LOCALIZED_POLICY,
        StorefrontRoutes.PAGE, StorefrontRoutes.LOCALIZED_PAGE);
```

A route added to `StorefrontRoutes`, the mapping and both `PublicRoute` entries but
**not here** is served ungated: a password-protected store would hand that page to an
anonymous visitor. That is precisely the leak the gate exists to prevent, and the
failure is silent — the page works, so every test and every curl passes.

**The coverage gap makes it concrete.** Locked-state assertions per controller:

| controller | `LockState.LOCKED` assertions |
|---|---|
| `StorefrontHomeControllerTest` | 3 |
| `PasswordPageControllerTest` | 1 |
| **`StorefrontCmsPageControllerTest`** | **0** |
| **`StorefrontPlaceholderControllerTest`** | **0** |

So **five of the eight gated addresses** — `PAGE`, `LOCALIZED_PAGE`, `EXPERIENCES`,
`LOCALIZED_EXPERIENCES`, `POLICY`/`LOCALIZED_POLICY` — have no test proving the gate
fires on them. The gate works on them today because the interceptor list happens to be
right; nothing would notice if it stopped being.

The one test that does enumerate addresses hardcodes them
(`EVERY_ADDRESS = { StorefrontRoutes.EXPERIENCES, "/es/experiences", "/policies/terms",
"/es/policies/cancellation" }`), so a new route does not extend it either.

**Fix:** an architecture-style test that derives the set from `StorefrontRoutes` and
asserts each page constant appears in `StorefrontPublicRoutes` (GET **and** HEAD) and
in the interceptor's patterns — with `PASSWORD` as the one declared exception, since
gating the gate would loop. Reflection over the two registrars plus the constant list;
it fails the day a route is added to three registries out of four. ~2 hours including
a mutation check that removing one pattern actually reddens it.

Verified by: the four registries extracted and compared in this session
(`grep -oE 'public static final String [A-Z_]+'` on `StorefrontRoutes`;
`StorefrontRoutes\.[A-Z_]+` occurrences in the controllers, in
`StorefrontPublicRoutes` paired with `HttpMethod\.[A-Z]+`, and in `StorefrontWebConfig`);
`grep -c "LockState.LOCKED"` per controller test; `StorefrontWebConfig.java` and
`StorefrontPlaceholderControllerTest.java:49-52` read.

### 3.2 The rate-limit registry hardcodes the password path

**`src/main/java/com/vointika/storefront/infrastructure/security/StorefrontRateLimitRoutes.java:15`** ·
**medium**

```java
new RateLimitRule(HttpMethod.POST, "/password", 20, Duration.ofHours(1))
```

Every other registry references `StorefrontRoutes.PASSWORD`; this one repeats the path
as a string literal. It is a **fifth** member of the must-agree set that `StorefrontRoutes`
exists to prevent — its own Javadoc says so: *"Every address the storefront answers
on, defined once because two registries have to agree on each of them."*

Rename the constant and the mapping, the `PublicRoute` entries and the interceptor all
follow; the rate limit silently stops matching. What is lost is the **20/h per-IP
brute-force limit on the password POST** — the only control standing between an
attacker and an unlimited guess rate against a store's password, and its absence looks
exactly like success.

**Fix:** use `StorefrontRoutes.PASSWORD`. One line. ~5 minutes. The constant is already
imported into this package's sibling classes.

Verified by: full file read (above);
`grep -oE 'public static final String [A-Z_]+' StorefrontRoutes.java` confirming
`PASSWORD` exists; `StorefrontRateLimitRoutesTest` exists but asserts the rule's
contents, not that the path tracks the constant.

### 3.3 The media-resolution block is copy-pasted across both real controllers

**`StorefrontHomeController.java` / `StorefrontCmsPageController.java`** · **low**

Four lines, identical modulo one accessor path:

```java
// StorefrontHomeController.render
Set<UUID> mediaIds = StorefrontGlobalsResponse.mediaIds(globals);
Map<UUID, MediaAsset> assets = mediaIds.isEmpty()
        ? Map.of()
        : mediaAssetBatchQuery.findAssetsByIds(globals.tourOperator().id(), mediaIds);
```

```java
// StorefrontCmsPageController.render
Set<UUID> mediaIds = StorefrontGlobalsResponse.mediaIds(output.globals());
Map<UUID, MediaAsset> assets = mediaIds.isEmpty()
        ? Map.of()
        : mediaAssetBatchQuery.findAssetsByIds(output.globals().tourOperator().id(), mediaIds);
```

The empty-set guard is the part worth sharing: it is what stops a page with no images
issuing a pointless batch query, and it is the kind of thing a third copy quietly
drops. `/experiences` and `/policies` become real routes later and will each want the
same block — that is the actual cost, not the eight lines today.

`StorefrontControllers` already exists as this package's shared-helper home, holding
`origin(...)` and `notFound()`.

**Fix:** move it there as
`assets(StorefrontGlobals, MediaAssetBatchQuery)`. ~20 minutes.

Verified by: both `render` methods read end to end and quoted above;
`StorefrontControllers.java` read in full.

---

## §4 · Categories with nothing to report

- **Over-engineering — none found.** `StorefrontProperties` is a one-field record
  (`baseDomain`) with one consumer, bound and used. There are no `Request`/`Input`
  pairs here (the context is read-only apart from the password POST). The
  application layer imports nothing outside `com.vointika..` + `java..`.
- **Bad practices — none found beyond §3.2.** No swallowed exceptions, no empty
  catches, no N+1: media resolves in one batch per request by construction
  (`mediaIds()` is deliberately kept beside `from()` so the two cannot drift).
- **Drift — none found.** All four route registries agree (§3.1 is that nothing
  *keeps* them agreeing, not that they diverge). `CLAUDE.md`'s "eight addresses" is
  accurate. The cutback left no stale explanations.

Verified by: as cited per line above.

---

## §5 · Comment noise

`storefront` is the densest context in the repo at **39.1%** (912 of 2333 lines), and
after PR #154 it contains **0 banners, 0 TODOs, 0 commented-out code**.

**I am not flagging the remaining density, and the reason is specific to this
context.** `storefront` has no domain layer, so its invariants live nowhere but its
policies — and three of those comments are the only record of a decision that cost
real debugging:

- **`StorefrontRoutes.LOCALE`** — that the locale variable must be *constrained* is a
  security decision, not a routing one: review of #91 measured a bare `/{locale}`
  taking `GET /error` from 401 to 200 by `permitAll`ing every single-segment path in
  the application. It also records that the group must be non-capturing because
  `PathPatternParser` throws outright. **Keep.**
- **`LocaleRule`** — why `/{primary}` is a 404 rather than a second address for the
  same page, and why every rejection is the same empty. That single decision is why
  `canonicalUrl` can be self-referencing. **Keep.**
- **`StorefrontLockInterceptor` / the gate ordering** — that the gate runs *before*
  locale resolution, and that reversing it tells an anonymous visitor which locales a
  locked store publishes. **Keep.**

Deleting any of these loses information no reader recovers from the code. The audit
brief's own KEEP list — "warnings about ordering", "tradeoffs, rejected approaches" —
covers all three.

Verified by: comment-ratio script over
`src/main/java/com/vointika/storefront/**/*.java`; `grep -rc` for banners and TODOs
→ 0 files each; the three cited Javadocs read in full.

---

## UNVERIFIED — needs human check

**Nothing.** Every claim rests on a command run or a file read in this session.

---

## The five I would fix first

1. **§3.1 — pin the four-registry agreement.** It is the only finding whose silent
   failure is a privacy leak: a page served ungated on a store the operator locked.
   The registries are correct today, which is exactly when a guard is cheap to add and
   worth the most.
2. **§3.2 — use `StorefrontRoutes.PASSWORD` in the rate-limit rule.** One line, and it
   removes the only remaining hardcoded copy of a path the whole context is organised
   around. What it protects is the password gate's brute-force limit.
3. **§3.3 — move the media block into `StorefrontControllers`.** Cheap, and it is the
   difference between two copies and four once `/experiences` and `/policies` become
   real.
4. **Add a locked-state test to `StorefrontCmsPageControllerTest`.** Partly subsumed by
   #1, but worth calling out separately: `/pages/{handle}` is the newest real route and
   the gate on it is currently unasserted at any level.
5. **Nothing else.** Dead code, coupling, over-engineering and drift all came back
   genuinely empty, and the comment density here is load-bearing rather than noise.
   Adding a fifth item would mean inventing one.
