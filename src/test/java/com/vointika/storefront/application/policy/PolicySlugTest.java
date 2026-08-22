package com.vointika.storefront.application.policy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The slug rule, both ways.
 *
 * <p>It exists in one place because two things must agree: the globals publish
 * {@code tourOperator.policies[].url}, and the route has to answer at exactly
 * that address. A second copy of the transform is how a link and its target drift
 * apart.
 */
class PolicySlugTest {

    /**
     * The real type names, read from the enum rather than retyped — {@code
     * storefront} may not import {@code PolicyType}, but a test may, and pinning
     * against the actual constants is what makes this cover a fifth policy type
     * on the day one is added.
     */
    private static Set<String> policyTypeNames() {
        return Arrays.stream(com.vointika.touroperator.domain.enums.PolicyType.values())
                .map(Enum::name)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    void everyPolicyTypeRoundTrips() {
        Set<String> names = policyTypeNames();
        assertThat(names).as("the enum walk found nothing, so this test checked nothing").isNotEmpty();

        for (String name : names) {
            String slug = PolicySlug.of(name);
            assertThat(PolicySlug.toTypeName(slug))
                    .as("%s -> %s -> back", name, slug)
                    .isEqualTo(name);
        }
    }

    /**
     * <b>Every slug must survive the route's own pattern</b>, or the page is
     * unreachable at an address the globals still advertise. The constraint is
     * lowercase letters and single hyphens.
     */
    @Test
    void everySlugMatchesTheRoutePattern() {
        for (String name : policyTypeNames()) {
            assertThat(PolicySlug.of(name))
                    .as("%s must be addressable by StorefrontRoutes.POLICY", name)
                    .matches("[a-z]+(?:-[a-z]+)*");
        }
    }

    @Test
    void underscoresBecomeHyphensAndBack() {
        assertThat(PolicySlug.of("LEGAL_NOTICE")).isEqualTo("legal-notice");
        assertThat(PolicySlug.toTypeName("legal-notice")).isEqualTo("LEGAL_NOTICE");
    }

    /**
     * An unknown slug maps to a name no enum constant has, which is how the read
     * answers empty without this class carrying a copy of the enum.
     */
    @Test
    void anUnknownSlugMapsToANameNoTypeHas() {
        assertThat(PolicySlug.toTypeName("refunds")).isEqualTo("REFUNDS");
        assertThat(policyTypeNames()).doesNotContain("REFUNDS");
    }

    /**
     * <b>Locale.ROOT on both sides.</b> Under a Turkish default locale
     * {@code "LEGAL_NOTICE".toLowerCase()} is {@code "legaı_notice"} — dotless ı —
     * so the page would 404 on the machine that served it. The guard scans source,
     * but this pins the behaviour (PATTERNS §11).
     */
    @Test
    void theTransformIsLocaleIndependent() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr"));
            assertThat(PolicySlug.of("LEGAL_NOTICE")).isEqualTo("legal-notice");
            assertThat(PolicySlug.toTypeName("legal-notice")).isEqualTo("LEGAL_NOTICE");
        } finally {
            Locale.setDefault(original);
        }
    }

    /**
     * The forward rule has exactly one implementation <b>in {@code storefront}</b>,
     * which is where re-inlining it would actually hurt: it was inline in
     * {@code StorefrontGlobalsResponse} until the route needed the inverse, and a
     * link and its target drifting apart is the failure.
     *
     * <p><b>Scoped, because the transform is generic and the claim is not.</b>
     * {@code replace('_', '-')} is a string operation, not a policy rule —
     * {@code NotificationType.fileBase()} uses the identical call to turn an enum
     * constant into a template filename, and an unscoped scan flags it. A guard
     * that cannot tell its target from an unrelated line is one that gets deleted
     * the first time it cries wolf (PATTERNS §9a: match on something only the
     * target has).
     */
    @Test
    void theTransformIsWrittenOnceInTheStorefront() throws Exception {
        java.nio.file.Path storefront = java.nio.file.Path.of("src/main/java/com/vointika/storefront");
        List<String> offenders = new java.util.ArrayList<>();
        int scanned = 0;
        try (var files = java.nio.file.Files.walk(storefront)) {
            for (java.nio.file.Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                scanned++;
                if (file.getFileName().toString().equals("PolicySlug.java")) {
                    continue;
                }
                String source = java.nio.file.Files.readString(file);
                if (source.contains("replace('_', '-')") || source.contains("replace('-', '_')")) {
                    offenders.add(file.getFileName().toString());
                }
            }
        }

        // A walk that reaches nothing passes the assertion below on an empty list.
        assertThat(scanned)
                .withFailMessage("Scanned %d storefront sources, so this guard checked nothing.", scanned)
                .isGreaterThan(10);
        assertThat(offenders)
                .withFailMessage("The policy slug transform appears outside PolicySlug: %s. Two copies is "
                        + "how a published url and the route serving it drift apart.", offenders)
                .isEmpty();
    }
}
