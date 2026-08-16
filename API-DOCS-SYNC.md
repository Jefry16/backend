# API-docs sync audit — the playbook

Check that one context's REST API and its Spring REST Docs documentation agree.
Investigation and reporting only. Do not change code or docs.

Invoke with a context name:

> **Run `API-DOCS-SYNC.md` on `page`.**

Run it context by context. **`storefront` goes last** — its surface is public,
unauthenticated and still half placeholder, so it answers to different rules than the
admin API and is easiest to judge once the rest is settled.

## What this repo actually is

Verified 2026-08-16, so you do not have to rediscover it:

- **Maven, not Gradle.** `./mvnw`, and `-o` for offline once dependencies are cached.
- **Spring Web MVC only.** No WebFlux, so **no `RouterFunction` beans to hunt for**.
- **No actuator.** The starter was declared and dropped, so there are no custom
  actuator endpoints.
- **No path prefix.** `application.yml` sets `spring.servlet.multipart` and nothing
  else under `servlet`. There is no `server.servlet.context-path` and no
  `spring.mvc.servlet.path`, so a full path is the class-level `@RequestMapping` plus
  the method annotation, and nothing more.
- **Snippets land in `target/generated-snippets`** — the Maven default, not
  overridden anywhere in `pom.xml`.
- **There is exactly one Asciidoctor source: `src/docs/asciidoc/api-guide.adoc`**,
  about 1,400 lines. It renders to `target/generated-docs/api-guide.html` and is then
  copied into `static/docs`, so the packaged app serves it at `/docs/api-guide.html`.
- **The guide uses `operation::`, not `include::`.** There are 154 `operation::`
  macros and **zero** `include::` directives. The generic form of this audit tells you
  to inventory `include::` lines; here that returns nothing and means nothing. An
  `operation::name[]` macro pulls in every snippet under
  `target/generated-snippets/name/` at once.
- **`target/` is gitignored, so snippets are not in version control.** That half of
  the stale-artifact question is already answered. Do not report it.

## Read before you start

- `../CONSTITUTION.md` (LAW) and `../MAP.md` — MAP's header lists what is already
  open. Do not present any of it as a discovery.
- `CLAUDE.md` for the auth model and the test traps, `PATTERNS.md` §4a, §4b and §9
  for the response and documentation-test conventions.
- The previous `API-DOCS-AUDIT.md` if one exists. It is deleted once its findings
  ship, so its absence means the last pass is closed, not that none ran.

## What is already enforced, and what it cannot see

Two ArchUnit-style guards live in `src/test/java/com/vointika/architecture/`. **Read
both before reporting anything**, or you will file findings the build already catches.

- **`ApiGuideDocumentsEveryEndpointTest`** — every `document("…")` literal in the test
  sources has an `operation::` line in the guide, and every `operation::` line has a
  `document(…)` behind it. It also fails on a duplicate `operation::` line.
- **`ApiGuideDocumentsEveryListFieldTest`** — every filterable and sortable field on a
  `ListSchema` is named in that endpoint's guide section, split at the sort clause so a
  field documented only as a sort cannot satisfy its filter entry.

**Its blind spot is the whole point of this audit.** The endpoint guard compares
`document(…)` literals to `operation::` lines. Both are documentation-side facts. It
cannot see a controller with **no documenting test at all** — that endpoint is absent
from both sides, so both sides agree, and the build is green. **Finding A is where the
real work is.**

Five review findings on `ApiGuideDocumentsEveryListFieldTest` exist only in PR #163's
description: filter operators undocumented, `sectionFor`'s `start < 0 ? 0` fallback,
the missing-clause assertion throwing inside `forEach`, `SCHEMA_BLOCK` silently
skipping an inline schema, and a dead `LinkedHashMap` copy. Read them before judging
that guard.

## Step 1 — the endpoint inventory, from the code

For the context under audit, find every request mapping under
`src/main/java/com/vointika/<context>/presentation/controller/`. For each, record:

method · full resolved path · path variables · query parameters · request body type ·
response body type · declared status codes · who may call it · which
`@ControllerAdvice` responses apply.

**Resolve the path yourself.** Concatenate the class-level `@RequestMapping` with the
method annotation. Do not read the method annotation alone and call it the path.

Two things to get right on this codebase:

- **Query parameters on a list endpoint are not `@RequestParam`s.** A list controller
  injects `ListQueryParser` and reads the raw request, so the accepted set is
  `sort`, `cursor` and `filter[field][op]`, taken from the use case's `SCHEMA` rather
  than from the method signature. Anything else is a 422. Read the schema.
- **Auth is two layers.** Every route under `/api/tour-operators/{id}/**` passes a
  membership interceptor that answers **404** to a non-member, byte-identical to a
  missing operator. Role gates (`ensureAdmin` / `ensureOwner`) then live in the use
  case, not the controller. So the roles an endpoint requires are found by reading the
  use case, and the router tells you less than it appears to.

Flag separately, do not mix into the main list: anything in a dev-profile or test-only
controller.

## Step 2 — the documented inventory, from the docs

Regenerate from scratch. Stale snippets on disk are the classic way this audit lies to
itself, and `target/generated-snippets` keeps directories for tests that no longer
exist:

```
./mvnw -o clean package
```

**`package`, not `test`.** Asciidoctor is bound to `prepare-package`, so `test` alone
generates snippets and never renders the guide.

Then inventory: every directory under `target/generated-snippets`, every
`operation::` macro in `src/docs/asciidoc/api-guide.adoc`, and the rendered
`target/generated-docs/api-guide.html`.

## Step 3 — reconcile

Report each as its own list. Every finding carries `file:line` and a **Verified by:**
line naming the command you ran or the file you read.

**A. UNDOCUMENTED ENDPOINTS** — in the code, no documenting test, no snippet. The
failure mode neither guard can catch. Highest priority.

**B. ORPHANED SNIPPETS** — a snippet directory a test generated that no `operation::`
macro pulls in. The test passes, the guide builds, the reader never sees it.

**C. BROKEN MACROS** — an `operation::` naming a snippet directory that does not exist
after a clean build. **Report whether the build fails or renders a broken page**, and
say which you observed rather than which you expect.

**D. STALE ARTIFACTS** — snippets or guide sections for endpoints that no longer
exist. Skip the gitignore question; it is answered above.

**E. RELAXED DOCUMENTATION** — every `relaxedRequestFields`, `relaxedResponseFields`,
`relaxedRequestParameters`, `relaxedPathParameters`, `relaxedQueryParameters`. Each
suppresses the strict check, so a field can exist in the response, be absent from the
guide, and the build stays green.

For each one, **read the actual record being serialized** — including inherited
components, `@JsonProperty` renames and `@JsonIgnore` — and list exactly which fields
are undocumented. Two conventions here will bite you: a response carries `id` and
`context` (PATTERNS §4a), and `context` has no Java caller at all, since only Jackson
reads it.

**F. PARTIAL COVERAGE** — the endpoint has a documenting test that only covers the
happy path. Report where documented status codes do not match what the controller and
`GlobalExceptionHandler` can actually return, where the error shape is undocumented,
and where auth failures are undocumented. On a tenant-scoped route the interesting one
is **404 for a non-member**, not 403.

**G. PROSE DRIFT** — hand-written text in the guide that contradicts the code. Stale
enum lists, defaults that moved, deprecated endpoints described as current, wrong
field semantics. Quote the guide line and the code line side by side.

**H. DESCRIPTION QUALITY** — `fieldWithPath(...).description(...)` entries that
restate the field name. "id" described as "The id" is documented in form only.

## Verification — no assumptions

Every finding comes from something you ran or read. This is LAW §4 and it is absolute.

- **To claim an endpoint is undocumented**, grep the whole test tree for its path, its
  handler method name, and its controller class, then confirm no snippet directory
  exists for it. **Show the searches.**
- **Never run `docker` or `docker compose`.** If a claim needs the live stack, write
  the exact command for the user and put the claim in UNVERIFIED.
- **Measure in a scratch copy** — the recipe is in `CLAUDE.md`, and `target/` is
  sometimes left root-owned by the Docker build.
- If you cannot verify something, put it under **UNVERIFIED — needs human check** with
  what blocked you. Never assert an unverified claim, and never soften one by hedging
  in the prose. A finding is verified and asserted, or it is in the unverified
  section. Nothing in between.

## Do not flag these

- **Snippets are not committed.** `target/` is gitignored.
- **`context` has no Java caller.** Jackson reads it; it is the §4a wire contract.
- **A list endpoint declares no `@RequestParam`.** That is `ListQueryParser`'s design.
- **Reference and ui-language lists return plain arrays.** Curated and bounded,
  exempt from §4b by decision.
- **The 19 body-returning endpoints with no field table.** Already in MAP's Debt.

## Output

A table of every endpoint in the context:

| method | path | has test | has snippet | in the guide | strict or relaxed | status |

Then the lettered findings. Then **the gaps to close first, ordered by how likely a
consumer of this API is to be misled** — not by how easy they are to fix.

Write it to **`API-DOCS-AUDIT.md`**, replacing whatever is there. Note in the header
which context this pass covered and which passes came before it.

**The report is tracked.** Commit it on the branch that produces it, staging explicit
paths — `git add -- API-DOCS-AUDIT.md src …`, **never `git add -A`**. Delete it once
its findings have shipped: a report describing fixed defects is a doc that contradicts
the code. Anything durable belongs in `../MAP.md`, not in the report.

## How the report must read

Write it for a working engineer skimming on a phone.

- **Say the thing first.** Every finding opens with what is wrong, in one sentence, in
  ordinary words.
- **One idea per sentence.** Aim for 15 words, not 30. If a sentence needs three
  clauses, it is three sentences.
- **No jargon and no hedging.** Not "this constitutes a latent documentation gap" —
  "a client cannot discover this filter". "Might", "could arguably", "it seems" and
  "somewhat" are banned in findings.
- **No throat-clearing.** Delete "it is worth noting that" and keep the point.
- **Quantities, not adjectives.** "9 of 12 endpoints" beats "coverage is weak".
- **Say who it hurts.** A finding a reader cannot act on is not finished.
- **Own mistakes in one line** and move on.
- **An empty category is a fine result.** Say "none found" rather than inventing
  something.
