package com.vointika.storefront.application.policy;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The locale rule decides which addresses exist, so every case here is a URL
 * that either resolves or 404s — not a preference.
 */
class LocaleRuleTest {

    private static final Set<String> SUPPORTED = Set.of("es", "en", "fr");

    @Test
    void theBarePathServesThePrimary() {
        assertThat(LocaleRule.resolve(null, "es", SUPPORTED)).contains("es");
    }

    @Test
    void aSupportedSecondaryServesItself() {
        assertThat(LocaleRule.resolve("en", "es", SUPPORTED)).contains("en");
        assertThat(LocaleRule.resolve("fr", "es", SUPPORTED)).contains("fr");
    }

    /**
     * The primary already lives at {@code /}, and two URLs for one page is
     * duplicate content. This is the case that looks like a bug until you know
     * why, which is why it is pinned rather than left to the reader.
     */
    @Test
    void thePrimaryHasNoPrefixedAddress() {
        assertThat(LocaleRule.resolve("es", "es", SUPPORTED)).isEmpty();
    }

    @Test
    void aLocaleTheShopDoesNotSupportIsNotAnAddress() {
        assertThat(LocaleRule.resolve("de", "es", SUPPORTED)).isEmpty();
    }

    /**
     * The route pattern only matches lowercase, so this can't arrive over HTTP
     * today — the rule holds it on its own so that stays true if the pattern
     * ever widens.
     */
    @Test
    void theComparisonIsExact() {
        assertThat(LocaleRule.resolve("EN", "es", SUPPORTED)).isEmpty();
    }

    /** An operator whose supported set somehow lost its primary still serves {@code /}. */
    @Test
    void theBarePathDoesNotConsultTheSupportedSet() {
        assertThat(LocaleRule.resolve(null, "es", Set.of())).contains("es");
    }
}
