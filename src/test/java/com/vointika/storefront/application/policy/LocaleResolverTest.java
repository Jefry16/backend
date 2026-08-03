package com.vointika.storefront.application.policy;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * One test per row of the locale rule. The third is the one that looks like a
 * bug and is not: the primary locale 404s under a prefix because it already
 * lives at {@code /}, and serving it twice is duplicate content.
 */
class LocaleResolverTest {

    private static final Set<String> SUPPORTED = Set.of("es", "en", "fr");

    @Test
    void aBarePathRendersThePrimaryLocale() {
        assertThat(LocaleResolver.resolve(null, "es", SUPPORTED)).contains("es");
    }

    @Test
    void aSupportedSecondaryRendersItself() {
        assertThat(LocaleResolver.resolve("fr", "es", SUPPORTED)).contains("fr");
    }

    @Test
    void thePrimaryUnderAPrefixIsNotAPage() {
        assertThat(LocaleResolver.resolve("es", "es", SUPPORTED)).isEmpty();
    }

    @Test
    void anUnsupportedLocaleIsNotAPage() {
        assertThat(LocaleResolver.resolve("de", "es", SUPPORTED)).isEmpty();
    }

    /**
     * The primary is always in the supported set in the database (V4 backfills
     * it), so this is the shape of a row that got there another way — and it must
     * not turn every prefixed URL into a page.
     */
    @Test
    void anEmptySupportedSetStillServesTheBarePathAndNothingElse() {
        assertThat(LocaleResolver.resolve(null, "es", Set.of())).contains("es");
        assertThat(LocaleResolver.resolve("es", "es", Set.of())).isEmpty();
        assertThat(LocaleResolver.resolve("fr", "es", Set.of())).isEmpty();
    }

    /** Each locale gets exactly one URL — {@code /ES} is not a second address for {@code /es}. */
    @Test
    void theComparisonIsExact() {
        assertThat(LocaleResolver.resolve("FR", "es", SUPPORTED)).isEmpty();
    }
}
