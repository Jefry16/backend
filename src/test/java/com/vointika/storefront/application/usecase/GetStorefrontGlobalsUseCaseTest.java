package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceCardView;
import com.vointika.shared.port.StorefrontMetafieldQuery;
import com.vointika.shared.port.StorefrontMetafieldQuery.MetafieldView;
import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.BrandView;
import com.vointika.shared.port.StorefrontShopQuery.ShopLocalesView;
import com.vointika.shared.port.StorefrontShopQuery.ShopView;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetStorefrontGlobalsUseCaseTest {

    private static final UUID OPERATOR = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");

    private StorefrontShopQuery shopQuery;
    private StorefrontMetafieldQuery metafieldQuery;
    private StorefrontExperienceQuery experienceQuery;
    private GetStorefrontGlobalsUseCase useCase;

    @BeforeEach
    void setUp() {
        shopQuery = mock(StorefrontShopQuery.class);
        metafieldQuery = mock(StorefrontMetafieldQuery.class);
        when(metafieldQuery.findForOperator(any())).thenReturn(List.of());
        experienceQuery = mock(StorefrontExperienceQuery.class);
        when(experienceQuery.findFeatured(any(), anyString())).thenReturn(List.of());
        useCase = new GetStorefrontGlobalsUseCase(shopQuery, metafieldQuery, experienceQuery);
    }

    private void shopExists(String primary, Set<String> supported) {
        when(shopQuery.findLocales("acme"))
                .thenReturn(Optional.of(new ShopLocalesView(OPERATOR, primary, supported)));
        when(shopQuery.findShop(any(), anyString()))
                .thenAnswer(call -> Optional.of(shop("Acme Tours", null)));
    }

    private static ShopView shop(String name, String seoTitle) {
        return new ShopView(OPERATOR, name, "acme", "Palma", null, null,
                seoTitle, "The best sailing in Mallorca", null, null,
                "EUR", "€", "Europe/Madrid", "Madrid",
                new BrandView(null, null, null, null, null, null, List.of(), List.of(), List.of()),
                List.of());
    }

    @Test
    void aHandleNoOperatorOwnsHasNoGlobals() {
        when(shopQuery.findLocales("nope")).thenReturn(Optional.empty());

        assertThat(useCase.execute("nope", null)).isEmpty();
    }

    /**
     * The locale rule runs <b>before</b> the content read, so a 404 costs one
     * query rather than two — and, more to the point, the shop is never read for
     * an address that does not exist.
     */
    @Test
    void anAddressThatDoesNotExistNeverReadsTheShop() {
        shopExists("es", Set.of("es", "en"));

        assertThat(useCase.execute("acme", "de")).isEmpty();
        verify(shopQuery, never()).findShop(any(), anyString());
    }

    @Test
    void theBarePathRendersThePrimaryLocale() {
        shopExists("es", Set.of("es", "en"));

        StorefrontGlobals globals = useCase.execute("acme", null).orElseThrow();

        assertThat(globals.localization().current()).isEqualTo("es");
        assertThat(globals.localization().primary()).isEqualTo("es");
        verify(shopQuery).findShop(OPERATOR, "es");
    }

    @Test
    void aPrefixedPathRendersThatLocale() {
        shopExists("es", Set.of("es", "en"));

        StorefrontGlobals globals = useCase.execute("acme", "en").orElseThrow();

        assertThat(globals.localization().current()).isEqualTo("en");
        verify(shopQuery).findShop(OPERATOR, "en");
    }

    /**
     * A {@code Set} comes out of the database in no particular order, and a
     * switcher that reshuffles between requests is a bug report.
     */
    @Test
    void theLanguagesAreOrderedPrimaryFirstThenAlphabetically() {
        shopExists("es", Set.of("fr", "es", "en", "de"));

        StorefrontGlobals globals = useCase.execute("acme", null).orElseThrow();

        assertThat(globals.localization().supported()).containsExactly("es", "de", "en", "fr");
    }

    /** The home page has no object of its own, so the shop is the whole chain. */
    @Test
    void thePageTitleFallsBackToTheShopName() {
        when(shopQuery.findLocales("acme"))
                .thenReturn(Optional.of(new ShopLocalesView(OPERATOR, "es", Set.of("es"))));
        when(shopQuery.findShop(any(), anyString())).thenReturn(Optional.of(shop("Acme Tours", null)));

        assertThat(useCase.execute("acme", null).orElseThrow().pageTitle()).isEqualTo("Acme Tours");
    }

    @Test
    void thePageTitlePrefersTheShopsSeoTitle() {
        when(shopQuery.findLocales("acme"))
                .thenReturn(Optional.of(new ShopLocalesView(OPERATOR, "es", Set.of("es"))));
        when(shopQuery.findShop(any(), anyString()))
                .thenReturn(Optional.of(shop("Acme Tours", "Sailing day trips in Mallorca")));

        assertThat(useCase.execute("acme", null).orElseThrow().pageTitle())
                .isEqualTo("Sailing day trips in Mallorca");
    }

    /**
     * The values come from another context, so this is the seam that has to be
     * called with the operator — not with a locale, not with an owner id from a
     * path. There is nothing else to get wrong here, and it is one line away
     * from being silently empty on every page.
     */
    @Test
    void theOperatorsMetafieldsRideTheGlobals() {
        shopExists("es", Set.of("es"));
        when(metafieldQuery.findForOperator(OPERATOR)).thenReturn(List.of(
                new MetafieldView("custom", "opening-hours", "single_line_text", "Mon-Sat 09:00-18:00")));

        StorefrontGlobals globals = useCase.execute("acme", null).orElseThrow();

        assertThat(globals.metafields())
                .singleElement()
                .satisfies(m -> {
                    assertThat(m.namespace()).isEqualTo("custom");
                    assertThat(m.key()).isEqualTo("opening-hours");
                    assertThat(m.type()).isEqualTo("single_line_text");
                });
        verify(metafieldQuery).findForOperator(OPERATOR);
    }

    /** An address that does not exist reads nothing at all, metafields included. */
    @Test
    void anAddressThatDoesNotExistReadsNoMetafieldsEither() {
        shopExists("es", Set.of("es", "en"));

        assertThat(useCase.execute("acme", "de")).isEmpty();
        verify(metafieldQuery, never()).findForOperator(any());
    }

    /**
     * The cards are read <b>in the locale the rule chose</b>, not the primary and
     * not the path segment — a handle and a name are both translated, so passing
     * the wrong one shows a Spanish visitor English cards linking to English URLs.
     */
    @Test
    void featuredExperiencesAreReadInTheRenderedLocale() {
        shopExists("es", Set.of("es", "en"));
        when(experienceQuery.findFeatured(OPERATOR, "en")).thenReturn(List.of(
                new ExperienceCardView(OPERATOR, "sunset-sail", "Sunset sail", "Sail into the sunset",
                        new java.math.BigDecimal("95.00"), null)));

        StorefrontGlobals globals = useCase.execute("acme", "en").orElseThrow();

        assertThat(globals.featuredExperiences()).singleElement()
                .satisfies(card -> assertThat(card.handle()).isEqualTo("sunset-sail"));
        verify(experienceQuery).findFeatured(OPERATOR, "en");
    }

    @Test
    void anAddressThatDoesNotExistReadsNoExperiencesEither() {
        shopExists("es", Set.of("es", "en"));

        assertThat(useCase.execute("acme", "de")).isEmpty();
        verify(experienceQuery, never()).findFeatured(any(), anyString());
    }
}
