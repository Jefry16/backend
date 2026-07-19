package com.vointika.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Boundary rules for vointika's DDD + hexagonal layout.
 *
 * <p>Bounded contexts are isolated from each other — they communicate via events
 * (Kafka, over {@code shared}) or shared query ports, never direct imports. Only
 * {@code shared} may be imported by every context. New per-context isolation
 * rules are added here as contexts land; today only {@code identity} and
 * {@code notification} exist.
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

    // ------------------------------------------------------------
    // Cross-context boundaries
    // ------------------------------------------------------------

    @ArchTest
    static final ArchRule shared_does_not_depend_on_any_bounded_context =
            noClasses()
                    .that().resideInAPackage("com.vointika.shared..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage(
                            "com.vointika.identity..",
                            "com.vointika.notification.."
                    )
                    .because("shared is the base module — it must not know about any bounded context");

    @ArchTest
    static final ArchRule identity_does_not_depend_on_other_bounded_contexts =
            noClasses()
                    .that().resideInAPackage("com.vointika.identity..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.vointika.notification..")
                    .because("bounded contexts communicate via events (shared) or shared kernel, not direct imports");

    @ArchTest
    static final ArchRule notification_does_not_depend_on_other_bounded_contexts =
            noClasses()
                    .that().resideInAPackage("com.vointika.notification..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.vointika.identity..")
                    .because("bounded contexts communicate via events (shared) or shared kernel, not direct imports");

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

    // ------------------------------------------------------------
    // Layered architecture within each bounded context
    // ------------------------------------------------------------

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

    // ------------------------------------------------------------
    // Domain purity
    // ------------------------------------------------------------

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
}
