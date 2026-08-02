package com.vointika.rendering.application.service;

import com.vointika.rendering.application.dto.output.Seo;
import com.vointika.shared.port.StorefrontOperatorTranslationView;
import com.vointika.shared.port.StorefrontOperatorView;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The home page's SEO chain, one step at a time.
 *
 * <p>A fallback chain is the shape that looks tested and is not: with every
 * override populated, deleting a step changes nothing and a single happy-path
 * assertion still passes. So each test here removes exactly one level and asserts
 * the <em>next</em> one wins — the only arrangement in which a missing step fails.
 */
class SeoResolverTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    /** An operator with every level populated; tests null out one at a time. */
    private StorefrontOperatorView shop(String seoTitle, String seoDescription,
                                        String ogImageUrl, String logoUrl,
                                        StorefrontOperatorTranslationView es) {
        return new StorefrontOperatorView(
                OP, "Acme Tours", "acme", logoUrl, "en", List.of("en", "es"),
                "USD", "America/Santo_Domingo", false, "canonical gate copy",
                seoTitle, seoDescription, ogImageUrl,
                es == null ? Map.of() : Map.of("es", es));
    }

    private static StorefrontOperatorTranslationView es(String title, String description, String message) {
        return new StorefrontOperatorTranslationView(title, description, message);
    }

    // ---- title: translated → canonical → shop name ----

    @Test
    void titlePrefersTheTranslatedOverride() {
        Seo seo = SeoResolver.forHome(shop("Canonical", "d", null, null, es("Traducido", null, null)), "es");
        assertThat(seo.title()).isEqualTo("Traducido");
    }

    @Test
    void titleFallsBackToTheCanonicalOverrideWhenTheLocaleIsUntranslated() {
        Seo seo = SeoResolver.forHome(shop("Canonical", "d", null, null, es(null, null, null)), "es");
        assertThat(seo.title()).isEqualTo("Canonical");
    }

    @Test
    void titleFallsBackToTheShopNameWhenNoOverrideExists() {
        // The home page has no content object, so the name is its last resort.
        Seo seo = SeoResolver.forHome(shop(null, "d", null, null, null), "en");
        assertThat(seo.title()).isEqualTo("Acme Tours");
    }

    // ---- description: translated → canonical → null ----

    @Test
    void descriptionPrefersTheTranslatedOverride() {
        Seo seo = SeoResolver.forHome(shop("t", "Canonical", null, null, es(null, "Traducida", null)), "es");
        assertThat(seo.description()).isEqualTo("Traducida");
    }

    @Test
    void descriptionFallsBackToTheCanonicalOverride() {
        Seo seo = SeoResolver.forHome(shop("t", "Canonical", null, null, es(null, null, null)), "es");
        assertThat(seo.description()).isEqualTo("Canonical");
    }

    @Test
    void descriptionIsNullWhenTheOperatorHasSetNone() {
        // Deliberately not invented: the shop name is a title, not a description.
        Seo seo = SeoResolver.forHome(shop("t", null, null, null, null), "en");
        assertThat(seo.description()).isNull();
    }

    // ---- image: og image → logo → null ----

    @Test
    void imagePrefersTheOgImage() {
        Seo seo = SeoResolver.forHome(shop("t", "d", "https://cdn/og.png", "https://cdn/logo.png", null), "en");
        assertThat(seo.imageUrl()).isEqualTo("https://cdn/og.png");
    }

    @Test
    void imageFallsBackToTheLogo() {
        Seo seo = SeoResolver.forHome(shop("t", "d", null, "https://cdn/logo.png", null), "en");
        assertThat(seo.imageUrl()).isEqualTo("https://cdn/logo.png");
    }

    @Test
    void imageIsNullWhenTheOperatorHasNeither() {
        assertThat(SeoResolver.forHome(shop("t", "d", null, null, null), "en").imageUrl()).isNull();
    }

    // ---- the shape of "absent" ----

    @Test
    void aBlankOverrideCountsAsAbsent() {
        // An empty string reaching the wire would be a title of zero characters,
        // not the absence of one — the chain must step over it.
        Seo seo = SeoResolver.forHome(shop("   ", null, null, null, es("", null, null)), "es");
        assertThat(seo.title()).isEqualTo("Acme Tours");
    }

    @Test
    void anUnknownLocaleReadsAsUntranslatedRatherThanFailing() {
        Seo seo = SeoResolver.forHome(shop("Canonical", null, null, null, es("Traducido", null, null)), "fr");
        assertThat(seo.title()).isEqualTo("Canonical");
    }

    // ---- the gate copy, which became translatable with the same table ----

    @Test
    void passwordMessagePrefersTheTranslatedCopy() {
        assertThat(SeoResolver.passwordMessage(
                shop(null, null, null, null, es(null, null, "copia traducida")), "es"))
                .isEqualTo("copia traducida");
    }

    @Test
    void passwordMessageFallsBackToTheCanonicalCopy() {
        assertThat(SeoResolver.passwordMessage(
                shop(null, null, null, null, es(null, null, null)), "es"))
                .isEqualTo("canonical gate copy");
    }
}
