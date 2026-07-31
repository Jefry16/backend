package com.vointika.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import java.util.Set;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.belongToAnyOf;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Boundary rules for vointika's DDD + hexagonal layout.
 *
 * <p>Bounded contexts are isolated from each other — they communicate via events
 * (Kafka, over {@code shared}) or shared query ports, never direct imports. Only
 * {@code shared} may be imported by every context, and {@code reference} is a
 * shared-kernel-like read module other contexts may import (it imports none).
 * New per-context isolation rules are added here as contexts land:
 * {@code identity}, {@code notification}, {@code reference}, {@code touroperator}.
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
                            "com.vointika.notification..",
                            "com.vointika.reference..",
                            "com.vointika.touroperator.."
                    )
                    .because("shared is the base module — it must not know about any bounded context");

    @ArchTest
    static final ArchRule identity_does_not_depend_on_other_bounded_contexts =
            noClasses()
                    .that().resideInAPackage("com.vointika.identity..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.vointika.notification..", "com.vointika.touroperator..")
                    .because("bounded contexts communicate via events (shared) or shared kernel, not direct imports");

    @ArchTest
    static final ArchRule notification_does_not_depend_on_other_bounded_contexts =
            noClasses()
                    .that().resideInAPackage("com.vointika.notification..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.vointika.identity..", "com.vointika.touroperator..")
                    .because("bounded contexts communicate via events (shared) or shared kernel, not direct imports");

    // touroperator owns the tenant aggregate. It may import the shared kernel and
    // the reference module (timezone/currency validation), but not identity or
    // notification — it reaches those via shared query ports / events.
    @ArchTest
    static final ArchRule touroperator_does_not_depend_on_other_bounded_contexts =
            noClasses()
                    .that().resideInAPackage("com.vointika.touroperator..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.vointika.identity..", "com.vointika.notification..")
                    .because("bounded contexts communicate via events (shared) or shared kernel, not direct imports");

    // reference is a read-mostly, shared-kernel-like module (countries, timezones):
    // other contexts may import it, but it depends on none of them.
    @ArchTest
    static final ArchRule reference_does_not_depend_on_other_bounded_contexts =
            noClasses()
                    .that().resideInAPackage("com.vointika.reference..")
                    .should().dependOnClassesThat()
                    .resideInAnyPackage("com.vointika.identity..", "com.vointika.notification..", "com.vointika.touroperator..")
                    .because("reference feeds other contexts (shared kernel), never the other way");

    // rendering assembles the public storefront's read models. It is the one
    // context that imports NOTHING but shared — not even reference — so every
    // fact a storefront page shows arrives through a shared port, already
    // resolved by the context that owns it. Stated as "outside rendering and
    // shared" rather than a list of contexts, so a context added later is
    // fenced off the day it appears.
    @ArchTest
    static final ArchRule rendering_depends_only_on_shared =
            noClasses()
                    .that().resideInAPackage("com.vointika.rendering..")
                    .should().dependOnClassesThat(
                            resideInAPackage("com.vointika..")
                                    .and(not(resideInAnyPackage(
                                            "com.vointika.rendering..",
                                            "com.vointika.shared.."))))
                    .because("rendering composes storefront read models from shared ports only — "
                            + "it must never reach into a bounded context directly");

    // contact owns the shopper inbox. It reads the tenant through a shared port
    // (the intake resolves a storefront by slug) and imports no bounded context —
    // the rule it shipped without in #63, added now that it has a second surface.
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

    /**
     * The application layer's entire legal dependency surface: our own code, the JDK,
     * and the SLF4J facade. An <strong>allowlist</strong>, deliberately — a list of
     * banned frameworks only catches the ones someone thought to name, which is how a
     * Jackson dependency under {@code tools.jackson} (not {@code com.fasterxml})
     * survived a grep. Anything outside this is coupling, including a library nobody
     * has imported yet.
     */
    private static final String[] APPLICATION_MAY_DEPEND_ON = {
            "com.vointika..", "java..", "org.slf4j.."
    };

    /**
     * The 21 use cases catching Spring's {@code DataIntegrityViolationException} to turn
     * a lost DB-unique race into a 409, plus the validator holding a Jackson
     * {@code ObjectMapper}. Frozen <em>by name</em>: a 22nd class cannot join without
     * editing this list. The previous version exempted the exception type instead, which
     * let the pattern spread silently — proven by adding the import to a 22nd use case
     * and watching the suite stay green. Both are debt (see MAP); the fix is to translate
     * in the repository adapter and delete this list.
     */
    /**
     * One class left. {@code MetafieldValueValidator} holds a Jackson
     * {@code ObjectMapper} to reject trailing-token garbage in {@code json} values —
     * debt, not a pattern; it wants a parser port.
     *
     * <p>The 21 use cases that used to sit here caught Spring's
     * {@code DataIntegrityViolationException} directly. They now catch
     * {@code UniqueConstraintViolationException}, translated once in
     * {@code SpringTransactionRunner}, which is where the flush actually fails.
     */
    private static final Set<String> FRAMEWORK_CATCHERS_FROZEN = Set.of(
            "com.vointika.metafield.application.service.MetafieldValueValidator"
    );

    private static final DescribedPredicate<JavaClass> IS_FROZEN =
            new DescribedPredicate<>("a frozen framework-catching class") {
                @Override
                public boolean test(JavaClass javaClass) {
                    return FRAMEWORK_CATCHERS_FROZEN.contains(javaClass.getFullName());
                }
            };

    @ArchTest
    static final ArchRule application_depends_only_on_our_code_the_jdk_and_slf4j =
            classes()
                    .that(resideInAPackage("com.vointika..application..").and(not(IS_FROZEN)))
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(APPLICATION_MAY_DEPEND_ON)
                    .because("anything else couples the layer to a framework — use a port");

    @ArchTest
    static final ArchRule frozen_classes_get_one_exemption_not_a_blank_cheque =
            classes()
                    .that(IS_FROZEN)
                    .should().onlyDependOnClassesThat(
                            resideInAnyPackage(APPLICATION_MAY_DEPEND_ON)
                                    .or(belongToAnyOf(
                                            tools.jackson.databind.ObjectMapper.class,
                                            tools.jackson.databind.JsonNode.class)))
                    .because("being on the frozen list buys one exemption, not free rein");
}
