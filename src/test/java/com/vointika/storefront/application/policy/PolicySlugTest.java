package com.vointika.storefront.application.policy;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
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
     * unreachable at an address the globals still advertise.
     *
     * <p>The pattern is <b>extracted from {@link StorefrontRoutes#POLICY}</b>, not
     * retyped beside it. A copy here would pass while the route rejected the slug:
     * tightening the constant to {@code {type:[a-z]+}} makes {@code legal-notice}
     * unaddressable, and a duplicated regex stays green through exactly that
     * change. Which would be this file arguing against copies six lines below one
     * of its own — see {@link #theTransformIsWrittenOnceInTheStorefront()}.
     */
    @Test
    void everySlugMatchesTheRoutePattern() {
        Pattern route = typeConstraint();

        for (String name : policyTypeNames()) {
            assertThat(route.matcher(PolicySlug.of(name)).matches())
                    .withFailMessage("PolicyType.%s slugs to '%s', which StorefrontRoutes.POLICY (%s) does "
                            + "not match - so the globals would publish a url the route 404s. Widen the "
                            + "constraint, or the slug rule is wrong.",
                            name, PolicySlug.of(name), StorefrontRoutes.POLICY)
                    .isTrue();
        }
    }

    /**
     * The {@code {name:regex}} constraint out of the route template, the way
     * {@code LocalePathTemplateTest.localeRegex()} does it for the locale.
     *
     * <p><b>An unconstrained variable fails loudly rather than matching
     * everything</b>, which is the failure mode a naive extraction has: no colon
     * would otherwise yield "anything goes" and this guard would pass on a route
     * that had lost its constraint entirely.
     *
     * <p>That case is now also covered as an invariant, over every constant at
     * once, by {@code StorefrontRouteRegistriesTest
     * .everyPathVariableInEveryRouteConstantIsConstrained} — which is where the
     * security property belongs, since it is a {@code PublicRoute} concern rather
     * than a slug one. The check stays here as a <b>precondition</b>: without it,
     * a template with no colon yields a nonsense regex compiled from the whole
     * path, and this test fails somewhere confusing instead of saying why.
     */
    private static Pattern typeConstraint() {
        String template = StorefrontRoutes.POLICY;
        int constraint = template.indexOf(':');
        assertThat(constraint)
                .withFailMessage("StorefrontRoutes.POLICY is '%s'. An unconstrained {type} is also the "
                        + "PublicRoute pattern, so it permitAlls every two-segment path under /policies "
                        + "rather than the four a policy can be. Constrain the variable.", template)
                .isNotNegative();
        return Pattern.compile(template.substring(constraint + 1, template.length() - 1));
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
