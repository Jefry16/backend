package com.vointika.storefront.presentation.response;

import com.vointika.shared.media.MediaUrlResolver;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.port.LocalizedHandles;
import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceDetailView;
import com.vointika.shared.port.StorefrontPageQuery.PageView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.AddressView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.BrandView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.TourOperatorView;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontGlobals.LocalizationData;
import com.vointika.storefront.application.policy.LocaleRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Every url the switcher mints is an address {@link LocaleRule} would resolve
 * back to the language it was offered for.
 *
 * <p><b>One round trip catches all three ways this goes wrong</b>, which is why
 * it is a rule rather than four hand-written expectations: minting
 * {@code /{primary}} (a 404 by {@code LocaleRule}, since the primary serves at
 * the bare path), minting a prefix for a locale the operator does not publish,
 * and failing to prefix a non-primary locale at all. Each would look plausible in
 * an assertion written by hand.
 *
 * <p>It runs over <b>every factory</b>. A new route that mints urls another way
 * is the case this exists to catch, and adding it here is one line.
 */
class SwitcherUrlsRoundTripThroughLocaleRuleTest {

    private static final UUID ID = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final String PRIMARY = "en";
    private static final List<String> SUPPORTED = List.of("en", "es", "fr");

    private static StorefrontGlobals globals(String current) {
        TourOperatorView operator = new TourOperatorView(ID, "Acme Tours", "acme",
                new AddressView(null, null, null, null, null, null, null, null),
                null, null, null, null, null, null, "EUR", "€", "Europe/Madrid", "Madrid",
                new BrandView(null, null, null, null, null, null, List.of(), List.of(), List.of()),
                List.of());
        return new StorefrontGlobals(operator, null, null, null, List.of(), List.of(), List.of(),
                new LocalizationData(current, PRIMARY, SUPPORTED));
    }

    private static final LocalizedHandles HANDLES =
            new LocalizedHandles("sunset-sail", Map.of("es", "paseo-al-atardecer"));

    private static ExperienceDetailView experience() {
        return new ExperienceDetailView(ID, "sunset-sail", "Sunset sail", "d", "l",
                new BigDecimal("1.00"), null, List.of(), null, null, false, 0,
                Instant.parse("2026-07-21T10:00:00Z"), null, HANDLES);
    }

    private static PageView page() {
        return new PageView(ID, "about-us", "About", "<p>x</p>", null, null,
                new LocalizedHandles("about-us", Map.of("es", "sobre-nosotros")));
    }

    private static List<Supplier<StorefrontGlobalsResponse>> everyFactory(String current) {
        MediaUrlResolver urls = mock(MediaUrlResolver.class);
        StorefrontGlobals globals = globals(current);
        return List.of(
                () -> StorefrontGlobalsResponse.index(globals, "https://acme.test", Map.of(), urls),
                () -> StorefrontGlobalsResponse.experienceList(globals,
                        new CursorPage<>(List.of(), null), "https://acme.test", Map.of(), urls),
                () -> StorefrontGlobalsResponse.cmsPage(globals, page(), "https://acme.test", Map.of(), urls),
                () -> StorefrontGlobalsResponse.experience(globals, experience(), List.of(),
                        "https://acme.test", Map.of(), urls));
    }

    @Test
    void everyOfferedUrlResolvesBackToTheLanguageItWasOfferedFor() {
        for (String current : SUPPORTED) {
            for (Supplier<StorefrontGlobalsResponse> factory : everyFactory(current)) {
                StorefrontGlobalsResponse response = factory.get();
                assertThat(response.localization().languages())
                        .as("serving %s", current)
                        .hasSize(SUPPORTED.size());

                for (StorefrontGlobalsResponse.Language language : response.localization().languages()) {
                    String pathLocale = firstSegmentIfALocale(language.url());
                    assertThat(LocaleRule.resolve(pathLocale, PRIMARY, new LinkedHashSet<>(SUPPORTED)))
                            .as("%s offered %s while serving %s", language.code(), language.url(), current)
                            .contains(language.code());
                }
            }
        }
    }

    /** The current language's entry is the route's canonical, by construction. */
    @Test
    void theCanonicalIsTheCurrentLanguagesOwnUrl() {
        for (String current : SUPPORTED) {
            for (Supplier<StorefrontGlobalsResponse> factory : everyFactory(current)) {
                StorefrontGlobalsResponse response = factory.get();
                assertThat(response.canonicalUrl())
                        .isEqualTo("https://acme.test" + response.localization().language().url());
                assertThat(response.localization().language().code()).isEqualTo(current);
            }
        }
    }

    /**
     * A url's first segment is a locale only when it is one the operator
     * publishes — otherwise it is the route ({@code /experiences}) or the page's
     * own handle, and the path locale is absent.
     */
    private static String firstSegmentIfALocale(String url) {
        String[] parts = url.split("/");
        if (parts.length < 2) {
            return null;
        }
        return SUPPORTED.contains(parts[1]) ? parts[1] : null;
    }

    /** Serving a locale the operator does not publish is refused where the reason is known. */
    @Test
    void servingAnUnsupportedLocaleFailsLoudly() {
        StorefrontGlobals broken = new StorefrontGlobals(globals("en").tourOperator(), null, null, null,
                List.of(), List.of(), List.of(), new LocalizationData("de", PRIMARY, SUPPORTED));

        assertThat(Optional.ofNullable(catchThrowable(() ->
                StorefrontGlobalsResponse.index(broken, "https://acme.test", Map.of(),
                        mock(MediaUrlResolver.class)))))
                .get()
                .isInstanceOf(IllegalStateException.class);
    }

    private static Throwable catchThrowable(Runnable r) {
        try {
            r.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }
}
