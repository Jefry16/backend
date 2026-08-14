package com.vointika.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Boundary rules for vointika's DDD + hexagonal layout.
 *
 * <p>Bounded contexts are isolated from each other — they communicate via events
 * (Kafka, over {@code shared}) or shared query ports, never direct imports. Only
 * {@code shared} may be imported by every context, and {@code reference} is a
 * shared-kernel-like read module other contexts may import; it imports nothing but
 * {@code shared} (the flag-URL resolver) — not "nothing", which this sentence
 * claimed until the {@code reference} audit checked it.
 * Context isolation is derived from the package structure, so a new context is
 * fenced the day it appears — no rule to remember to add.
 *
 * <p>Within a bounded context, layers form a DAG:
 * {@code domain} ← {@code application} ← {@code infrastructure} / {@code presentation}
 */
@AnalyzeClasses(
        packages = "com.vointika",
        importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class}
)
@SuppressWarnings("unused") // @ArchTest fields are discovered reflectively by ArchUnit's JUnit engine
public class ArchitectureTest {

    // Cross-context boundaries

    /**
     * Every bounded context is fenced from every other, derived from the package
     * structure rather than a hand-maintained list.
     *
     * <p>This replaced five per-context rules that each named the contexts they knew
     * about — written when there were four. Seven contexts landed afterwards and
     * nobody added rules for them, so half the codebase was unfenced; worse, the
     * rules that did exist only fenced against the original four, so
     * {@code touroperator → experience} passed too. Deriving the slices means the
     * next context is fenced the day its package appears.
     *
     * <p>{@code shared} and {@code reference} are the shared kernels: any context may
     * depend on them, which is why they are ignored as dependency <em>targets</em>.
     * What they may depend on is constrained by the rule below.
     */
    @ArchTest
    static final ArchRule contexts_do_not_depend_on_each_other =
            slices().matching("com.vointika.(*)..")
                    .should().notDependOnEachOther()
                    .ignoreDependency(
                            alwaysTrue(),
                            resideInAnyPackage("com.vointika.shared..", "com.vointika.reference.."))
                    .because("contexts communicate via shared query ports or Kafka events, never direct imports");

    /**
     * The base module knows nothing about anything above it — including
     * {@code reference}, which the generic rule above ignores as a target.
     */
    @ArchTest
    static final ArchRule shared_does_not_depend_on_any_bounded_context =
            noClasses()
                    .that().resideInAPackage("com.vointika.shared..")
                    .should().dependOnClassesThat(
                            resideInAPackage("com.vointika..")
                                    .and(not(resideInAPackage("com.vointika.shared.."))))
                    .because("shared is the base module — it must not know about any context");

    // contact owns the shopper inbox and imports no bounded context — the rule it
    // shipped without in #63. It reached the tenant through StorefrontOperatorQuery
    // until the storefront intake was removed on 2026-08-01; the admin side needs no
    // shared port at all now, so this rule currently has nothing to catch. Kept
    // because the intake returns, and a fence added back later is a fence that was
    // missing in between.
    @ArchTest
    static final ArchRule contact_depends_only_on_shared =
            noClasses()
                    .that().resideInAPackage("com.vointika.contact..")
                    .should().dependOnClassesThat(
                            resideInAPackage("com.vointika..")
                                    .and(not(resideInAnyPackage(
                                            "com.vointika.contact..",
                                            "com.vointika.shared.."))))
                    .because("contact reaches other contexts through shared ports only");

    // The Kafka client (producer/consumer/admin) is infrastructure for the event
    // backbone — confined to the shared producer/config package and the
    // notification consumer. Mirrors the Redis fence below.
    @ArchTest
    static final ArchRule kafka_client_only_in_shared_kafka_and_notification =
            noClasses()
                    .that().resideOutsideOfPackages(
                            "com.vointika.shared.infrastructure.kafka..",
                            "com.vointika.notification.infrastructure..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.apache.kafka..", "org.springframework.kafka..")
                    .allowEmptyShould(true)
                    .because("Kafka is wrapped by the shared event-publisher/config and the notification consumers only");

    // The Redis client (Spring Data Redis / Lettuce) is infrastructure for auth
    // rate limiting — confined to the shared redis package. Mirrors the Kafka fence.
    @ArchTest
    static final ArchRule redis_client_only_in_shared_redis =
            noClasses()
                    .that().resideOutsideOfPackage("com.vointika.shared.infrastructure.redis..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("org.springframework.data.redis..", "io.lettuce..")
                    .allowEmptyShould(true)
                    .because("Redis is wrapped by the shared redis rate-limiter only");

    // Layered architecture within each bounded context

    @ArchTest
    static final ArchRule layers_are_respected =
            Architectures.layeredArchitecture().consideringOnlyDependenciesInLayers()
                    .layer("Domain").definedBy("com.vointika.(*)..domain..")
                    .layer("Application").definedBy("com.vointika.(*)..application..")
                    .layer("Infrastructure").definedBy("com.vointika.(*)..infrastructure..")
                    .layer("Presentation").definedBy("com.vointika.(*)..presentation..")

                    .whereLayer("Presentation").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Infrastructure", "Presentation")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Infrastructure", "Presentation")

                    .as("Within a bounded context: domain ← application ← {infrastructure, presentation}. "
                            + "Domain must be pure; application may depend on domain; "
                            + "infrastructure and presentation may depend on domain and application.");

    // Domain purity

    @ArchTest
    static final ArchRule domain_does_not_depend_on_spring_or_jpa =
            noClasses()
                    .that().resideInAPackage("com.vointika..domain..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "org.hibernate..",
                            "com.fasterxml.jackson..",
                            "tools.jackson.."
                    )
                    .because("domain must stay pure — infrastructure concerns belong in infrastructure");

    /**
     * The application layer's entire legal dependency surface: our own code and the
     * JDK. Nothing else — not even a logging facade. An <strong>allowlist</strong>,
     * deliberately — a list of banned frameworks only catches the ones someone thought
     * to name, which is how a Jackson dependency under {@code tools.jackson} (not
     * {@code com.fasterxml}) survived a grep. Anything outside this is coupling,
     * including a library nobody has imported yet.
     *
     * <p>There are no exemptions, and there is no third-party library on the list.
     * Three were removed rather than tolerated: Spring's
     * {@code DataIntegrityViolationException} (translated once in
     * {@code SpringTransactionRunner}), Jackson (behind {@code JsonSyntaxPort}), and
     * SLF4J — best-effort side effects now log in the adapter that fails, and the
     * three use cases with something of their own to report go through
     * {@code DiagnosticLogPort}.
     */
    @ArchTest
    static final ArchRule application_depends_only_on_our_code_and_the_jdk =
            classes()
                    .that().resideInAPackage("com.vointika..application..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("com.vointika..", "java..")
                    .because("anything else couples the layer to a framework — use a port");
}
