# Vointika Backend — Stack & Docs Registry

**Every version-specific / load-bearing dependency → its exact version → the
official documentation for that version.** Read the docs here before implementing
or deciding anything version-specific (autoconfiguration, API shapes, config
keys, wiring). This exists because version-specific behavior is **not** reliably
recallable from training data — especially across a major-version boundary (see
the Boot 4 gotcha below). Constitution §4 makes consulting these a rule.

When you add or bump a listed dependency, add/adjust its row here in the same
change.

Not every jar in `pom.xml` gets a row: transitive libraries we never call
directly (`flyway-database-postgresql`, the `postgresql` JDBC driver) and pure
build/dev tooling (`lombok`, `spring-boot-devtools`) are intentionally omitted.
A dependency earns a row when we write code against its API, or when it develops
a version-specific gotcha.

## Runtime services (docker-compose, pinned tags)

| Component | Version | Official docs |
|---|---|---|
| PostgreSQL | `17.9` | https://www.postgresql.org/docs/17/ |
| Redis | `7.4.9` | https://redis.io/docs/latest/ |
| Apache Kafka (broker) | `4.3.1` | https://kafka.apache.org/documentation/ |
| MinIO (server) | `RELEASE.2025-09-07T16-13-09Z` | https://docs.min.io/ · https://github.com/minio/minio |
| MinIO client (`mc`) | `RELEASE.2025-08-13T08-35-41Z` | https://docs.min.io/ |
| SESv2 mock (`aws-ses-v2-local` on `node`) | `2.10.0` on `node:22.23.1-alpine` | https://github.com/domdomegg/aws-ses-v2-local — dev only; sent mail is readable at `GET localhost:8005/store` |

## Platform & framework (pom.xml)

| Component | Version | Official docs |
|---|---|---|
| Java (Temurin) | `25` | https://docs.oracle.com/en/java/javase/25/ · https://adoptium.net/temurin/ |
| Spring Boot | `4.0.5` | https://docs.spring.io/spring-boot/reference/ |
| Spring Framework | (via Boot 4.0.5) | https://docs.spring.io/spring-framework/reference/ |
| Spring Security | (via Boot 4.0.5) | https://docs.spring.io/spring-security/reference/ |
| Spring Web MVC | (via Boot 4.0.5) | https://docs.spring.io/spring-framework/reference/web/webmvc.html |
| Spring Data JPA / Hibernate | (via Boot 4.0.5) | https://docs.spring.io/spring-data/jpa/reference/ |
| Spring Data Redis (Lettuce) | (via Boot 4.0.5) | https://docs.spring.io/spring-data/redis/reference/index.html |
| Spring for Apache Kafka | `4.0.4` (via `spring-boot-starter-kafka`) | https://docs.spring.io/spring-kafka/reference/ |
| Kafka clients | `4.1.2` (via Boot BOM) | https://kafka.apache.org/documentation/ |
| Flyway | (via Boot 4.0.5) | https://documentation.red-gate.com/fd |

## Libraries (pom.xml)

| Component | Version | Official docs |
|---|---|---|
| JJWT (jjwt-api/impl/jackson) | `0.13.0` | https://github.com/jwtk/jjwt |
| AWS SDK for Java v2 (sesv2, s3) | `2.42.33` | https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/ · API: https://sdk.amazonaws.com/java/api/latest/ |
| uuid-creator | `6.1.1` | https://github.com/f4b6a3/uuid-creator |
| Thymeleaf (thymeleaf-spring6) | (via Boot 4.0.5) | https://www.thymeleaf.org/documentation.html |
| jmustache (`com.samskivert:jmustache`) | `1.16` (transitively via `spring-boot-starter-mustache` 4.0.5 → `spring-boot-mustache` 4.0.5) | https://github.com/samskivert/jmustache · Boot integration: https://docs.spring.io/spring-boot/reference/web/servlet.html#web.servlet.spring-mvc.template-engines |
| ArchUnit (test) | `1.4.1` | https://www.archunit.org/userguide/html/000_Index.html |
| Spring REST Docs (test) | `4.0.0` (mockmvc via Boot 4.0.5) | https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/ |
| spring-restdocs-asciidoctor (build) | `4.0.0` | https://docs.spring.io/spring-restdocs/docs/current/reference/htmlsingle/#working-with-asciidoctor |
| asciidoctor-maven-plugin (build) | `3.1.1` | https://docs.asciidoctor.org/maven-tools/latest/plugin/ |

## Version-specific gotchas (grows as we hit them)

Record every version-specific trap the moment it bites. That is what this file is
for: the next session reads it instead of re-discovering it.

- **Spring Boot 4 modularized autoconfiguration.** The raw
  `org.springframework.kafka:spring-kafka` library has **no** autoconfiguration in
  Boot 4. No `KafkaTemplate`, no producer or consumer factories, and `spring.kafka.*`
  is ignored. Depend on **`org.springframework.boot:spring-boot-starter-kafka`**
  instead, which brings the `spring-boot-kafka` autoconfiguration module. Documented
  at https://docs.spring.io/spring-boot/reference/messaging/kafka.html and tracked in
  [spring-boot#49207](https://github.com/spring-projects/spring-boot/issues/49207) and
  [spring-kafka#4278](https://github.com/spring-projects/spring-kafka/issues/4278).
  The same applies to every other starter (`spring-boot-starter-webmvc`, `-data-jpa`,
  `-data-redis`, …). Always add the **Boot starter**, never the raw library.

- **The autoconfigured `KafkaTemplate` is typed `KafkaTemplate<?, ?>`.** A
  `KafkaTemplate<String, Object>` injection point does not match it. Inject the raw
  `KafkaTemplate`; it is the single candidate by raw type.

- **jmustache's stock compiler throws on a null or missing variable.**
  `Mustache.compiler()` ships `nullValue=null` and `missingIsNull=false`. So
  `{{seoTitle}}` over an operator who never filled it in raises
  `MustacheException.Context` — a 500, not an empty string. `.defaultValue("")` fixes
  both. Sections are already lenient. Confirmed by reverting the setting and watching
  the test error, not by reading.

- **`new DefaultCollector(false)` also removes the JavaBean-getter fallback.** Turning
  access coercion off is the fix for the collector reading *private* fields and
  methods. But the same flag changes two lookups: `getMethod` drops from the
  `name()`/`get<Name>()`/`is<Name>()` search to a plain `clazz.getMethod(name)`, and
  `getField` to `clazz.getField(name)`.

  So a context object must expose **exactly-named public accessors**. A `record`
  works. `getOperatorName()` for `{{operatorName}}` does not, and it fails by
  rendering an empty page rather than by throwing.

  **A default *interface* method still resolves**, which reading the source suggests
  it should not. `getIfaceMethod` really is dead with coercion off — it ends in
  `makeAccessible`, which returns null. Nothing reaches it, because `clazz.getMethod`
  already returns inherited public interface methods. Both halves are pinned in
  `StorefrontMustacheConfigTest`. The interface half was settled by running it, after
  two readings of the same source disagreed.

- **A view model reached reflectively must be `public`, enclosing types included.**
  With coercion off, `Method.invoke` on a public accessor of a package-private class
  throws `IllegalAccessException` at render time. A `public` class nested in a
  package-private one counts as package-private here.

- **Boot 4 test slices are assembled per module.** `@WebMvcTest`'s autoconfiguration
  list is not one file. Each module contributes its own
  `META-INF/spring/…AutoConfigureWebMvc.imports`, so `spring-boot-mustache` registers
  `MustacheAutoConfiguration` into the web slice from its own jar. Reading only
  `spring-boot-webmvc-test`'s copy says the opposite. Same lesson as the Kafka starter
  above: check the module that owns the feature.

- **jmustache template inheritance works.**
  `{{<parent}} … {{$block}}…{{/block}} … {{/parent}}` is real
  (`ParentTemplateSegment` and `BlockSegment`), the close tag
  repeats the parent's full name (`{{/storefront/layout}}`), and it gives Dawn's
  `theme.liquid` plus `content_for_layout` shape natively. Two behaviours to know
  before writing a layout:

  1. **Anything inside the parent call that is not a block is discarded.**
     `removeNonBlocks` throws it away, so a child template is only its block
     definitions.
  2. **Whitespace between a block tag and its content is output.** Block tags have to
     hug the markup (`{{$content}}    <h1>…</h1>{{/content}}`) or the page gains blank
     lines.

  The parent is loaded by the *loader* on first render and pinned into the compiled
  `Template`, so a layout needs **no bean of its own** — one compiled graph per page
  template.

- **A Mustache comment cannot contain `}}`.** `{{! … }}` ends at the first `}}`, so a
  comment mentioning `{{$content}}` renders the rest of itself into the page. Found by
  doing it. Describe tags in words instead. Hit a second time in #100, by a comment
  quoting `{{url}}` — this entry existed and was not re-read. A whole-line `{{! … }}`
  **is** stripped cleanly, which is why the layout's other comments cost nothing.

- **A section tag is only standalone if it is alone on its line.** jmustache strips a
  line holding nothing but a tag. A line like `····{{#x}}<img …>{{/x}}` is not that, so
  when `x` is absent the four spaces and the newline are still emitted. The
  storefront's `{{#tourOperator.brand.logo}}` shipped a stray blank line that way. Fix
  it the same way as the block tags: hug. Open the section at the end of the previous
  line (`<header>{{#tourOperator.brand.logo}}`) so nothing is left when it is skipped.
  The footer's phone and email guards already did this; the rule is general.

- **Flyway ignores an applied migration whose version is *ahead* of everything local,
  and reports it the moment you add one past it.** A branch that lacks a migration
  another branch already applied to the dev database boots and tests green: the
  applied version is a "future" migration and validation skips it. Add a higher
  version on top and the same row becomes a gap —
  `Detected applied migration not resolved locally: 11` — which fails
  `flywayInitializer` and therefore the whole context. So
  `VointikaApplicationTests.contextLoads` is the only test that sees it. Two branches
  numbering migrations in the same context is the setup. The failure looks like the
  *new* migration's fault and is not. Read `<schema>.flyway_schema_history` before
  believing either.

- **Spring's `MustacheView` recompiles the template on every request.**
  `renderMergedTemplateModel` calls `compiler.compile(reader)`, and the caching view
  resolver above it caches the *View*, not the compiled `Template`. Fine for a few app
  templates, wrong for multi-tenant themes. So the storefront compiled once at startup
  and wrote the rendered string itself rather than returning a view name.

  **Every jmustache entry above is a finding without a live renderer behind it.** The
  storefront answers JSON until themes exist, so only `StorefrontMustacheConfig` and
  its test survive. These traps are here for the day the themes bring templates back.
