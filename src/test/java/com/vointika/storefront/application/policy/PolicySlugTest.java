package com.vointika.storefront.application.policy;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The one transform between the policy type's vocabulary and the URL's, tested
 * in both directions because both are used: inbound by the controller's path
 * variable, outbound by every footer link.
 */
class PolicySlugTest {

    /** The bare regex out of {@code /policies/&#123;type:<regex>&#125;}. */
    private static final Pattern SEGMENT = Pattern.compile(
            StorefrontRoutes.POLICY.substring(
                    StorefrontRoutes.POLICY.indexOf(':') + 1, StorefrontRoutes.POLICY.length() - 1));

    /** The interesting one: the enum name is not the address, and the underscore is why. */
    @Test
    void anUnderscoredTypeIsAddressedWithAHyphen() {
        assertThat(PolicySlug.of("LEGAL_NOTICE")).isEqualTo("legal-notice");
        assertThat(PolicySlug.toType("legal-notice")).isEqualTo("LEGAL_NOTICE");
    }

    @Test
    void aSingleWordTypeIsJustLowercase() {
        assertThat(PolicySlug.of("CANCELLATION")).isEqualTo("cancellation");
        assertThat(PolicySlug.toType("cancellation")).isEqualTo("CANCELLATION");
    }

    /**
     * <b>Every type is addressable and every address round-trips</b>, which is
     * what makes this a transform rather than a fourth copy of the closed set —
     * a type added to the enum needs nothing here.
     */
    @Test
    void everyTypeRoundTripsThroughAnAddressableSlug() {
        for (String type : new String[]{"CANCELLATION", "PRIVACY", "TERMS", "LEGAL_NOTICE"}) {
            String slug = PolicySlug.of(type);

            assertThat(SEGMENT.matcher(slug).matches())
                    .withFailMessage("The slug for %s is '%s', which StorefrontRoutes.POLICY does not "
                            + "match — the footer would link a page the router 404s, and nothing else "
                            + "would say why.", type, slug)
                    .isTrue();
            assertThat(PolicySlug.toType(slug)).isEqualTo(type);
        }
    }

    /**
     * <b>An unknown slug is not rejected here, deliberately.</b> It becomes a name
     * no type is called, the query port answers empty for it, and the page is a
     * 404 — which is the whole reason the port takes a name and looks it up
     * instead of calling {@code valueOf}.
     */
    @Test
    void aSlugNoTypeIsNamedAfterStillConvertsRatherThanThrowing() {
        assertThat(PolicySlug.toType("refunds")).isEqualTo("REFUNDS");
        assertThat(PolicySlug.toType("shipping-policy")).isEqualTo("SHIPPING_POLICY");
    }
}
