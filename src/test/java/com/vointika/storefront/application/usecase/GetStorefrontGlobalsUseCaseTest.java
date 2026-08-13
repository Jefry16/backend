package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceCardView;
import com.vointika.shared.port.StorefrontMenuQuery;
import com.vointika.shared.port.StorefrontMenuQuery.MenuItemView;
import com.vointika.shared.port.StorefrontMenuQuery.MenuView;
import com.vointika.shared.port.StorefrontMetafieldQuery;
import com.vointika.shared.port.StorefrontPageQuery;
import com.vointika.shared.port.StorefrontMetafieldQuery.MetafieldView;
import com.vointika.shared.port.StorefrontTourOperatorQuery;
import com.vointika.shared.port.StorefrontTourOperatorQuery.AddressView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.BrandView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.LocalesView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.TourOperatorView;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
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
    private static final AddressView ADDRESS = new AddressView(
            "Calle Mayor 1", null, "Calle Mayor 1", "Palma", "Illes Balears", "07001",
            "ES", "Spain");

    private StorefrontTourOperatorQuery operatorQuery;
    private StorefrontMetafieldQuery metafieldQuery;
    private StorefrontExperienceQuery experienceQuery;
    private StorefrontMenuQuery menuQuery;
    private StorefrontPageQuery pageQuery;
    private GetStorefrontGlobalsUseCase useCase;

    @BeforeEach
    void setUp() {
        operatorQuery = mock(StorefrontTourOperatorQuery.class);
        metafieldQuery = mock(StorefrontMetafieldQuery.class);
        when(metafieldQuery.findForOperator(any())).thenReturn(List.of());
        experienceQuery = mock(StorefrontExperienceQuery.class);
        when(experienceQuery.findFeatured(any(), anyString())).thenReturn(List.of());
        menuQuery = mock(StorefrontMenuQuery.class);
        when(menuQuery.findMenus(any(), anyString())).thenReturn(List.of());
        pageQuery = mock(StorefrontPageQuery.class);
        when(pageQuery.findPublishedHandles(any(), any(), anyString())).thenReturn(Map.of());
        useCase = new GetStorefrontGlobalsUseCase(operatorQuery, metafieldQuery, experienceQuery,
                menuQuery, pageQuery);
    }

    private void shopExists(String primary, Set<String> supported) {
        when(operatorQuery.findLocales("acme"))
                .thenReturn(Optional.of(new LocalesView(OPERATOR, primary, supported)));
        when(operatorQuery.findOperator(any(), anyString()))
                .thenAnswer(call -> Optional.of(operator("Acme Tours", null)));
    }

    private static TourOperatorView operator(String name, String seoTitle) {
        return new TourOperatorView(OPERATOR, name, "acme", ADDRESS, null, null,
                seoTitle, "The best sailing in Mallorca", null, null,
                "EUR", "€", "Europe/Madrid", "Madrid",
                new BrandView(null, null, null, null, null, null, List.of(), List.of(), List.of()),
                List.of());
    }

    @Test
    void aHandleNoOperatorOwnsHasNoGlobals() {
        when(operatorQuery.findLocales("nope")).thenReturn(Optional.empty());

        assertThat(useCase.execute("nope", null)).isEmpty();
    }

    /**
     * The locale rule runs <b>before</b> the content read, so a 404 costs one
     * query rather than two — and, more to the point, the operator is never read for
     * an address that does not exist.
     */
    @Test
    void anAddressThatDoesNotExistNeverReadsTheShop() {
        shopExists("es", Set.of("es", "en"));

        assertThat(useCase.execute("acme", "de")).isEmpty();
        verify(operatorQuery, never()).findOperator(any(), anyString());
    }

    @Test
    void theBarePathRendersThePrimaryLocale() {
        shopExists("es", Set.of("es", "en"));

        StorefrontGlobals globals = useCase.execute("acme", null).orElseThrow();

        assertThat(globals.localization().current()).isEqualTo("es");
        assertThat(globals.localization().primary()).isEqualTo("es");
        verify(operatorQuery).findOperator(OPERATOR, "es");
    }

    @Test
    void aPrefixedPathRendersThatLocale() {
        shopExists("es", Set.of("es", "en"));

        StorefrontGlobals globals = useCase.execute("acme", "en").orElseThrow();

        assertThat(globals.localization().current()).isEqualTo("en");
        verify(operatorQuery).findOperator(OPERATOR, "en");
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

    /** The home page has no object of its own, so the operator is the whole chain. */
    @Test
    void thePageTitleFallsBackToTheShopName() {
        when(operatorQuery.findLocales("acme"))
                .thenReturn(Optional.of(new LocalesView(OPERATOR, "es", Set.of("es"))));
        when(operatorQuery.findOperator(any(), anyString())).thenReturn(Optional.of(operator("Acme Tours", null)));

        assertThat(useCase.execute("acme", null).orElseThrow().pageTitle()).isEqualTo("Acme Tours");
    }

    @Test
    void thePageTitlePrefersTheShopsSeoTitle() {
        when(operatorQuery.findLocales("acme"))
                .thenReturn(Optional.of(new LocalesView(OPERATOR, "es", Set.of("es"))));
        when(operatorQuery.findOperator(any(), anyString()))
                .thenReturn(Optional.of(operator("Acme Tours", "Sailing day trips in Mallorca")));

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
                new MetafieldView("custom", "opening-hours", "single_line_text", "Mon-Sat 09:00-18:00", null)));

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

    /**
     * <b>The ids are collected across every menu before either lookup runs.</b> A
     * header and a footer both linking to the same page must cost one read
     * between them, not one each — and a menu with fifty items must still cost
     * two, not fifty.
     */
    @Test
    void everyMenusTargetsAreResolvedInTwoLookups() {
        shopExists("es", Set.of("es"));
        UUID experienceId = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e10");
        UUID pageId = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3e11");
        when(menuQuery.findMenus(OPERATOR, "es")).thenReturn(List.of(
                new MenuView("main-menu", "Main menu", List.of(
                        new MenuItemView(UUID.randomUUID(), null, "Sail", "EXPERIENCE", experienceId, null, 0),
                        new MenuItemView(UUID.randomUUID(), null, "About", "PAGE", pageId, null, 1))),
                new MenuView("footer", "Footer", List.of(
                        new MenuItemView(UUID.randomUUID(), null, "About", "PAGE", pageId, null, 0)))));
        when(experienceQuery.findPublishedHandles(OPERATOR, Set.of(experienceId), "es"))
                .thenReturn(Map.of(experienceId, "sunset-sail"));
        when(pageQuery.findPublishedHandles(OPERATOR, Set.of(pageId), "es"))
                .thenReturn(Map.of(pageId, "about-us"));

        StorefrontGlobals globals = useCase.execute("acme", null).orElseThrow();

        assertThat(globals.menus()).extracting("handle").containsExactly("main-menu", "footer");
        // The page id appears in both menus and is asked for once, as a set.
        verify(pageQuery).findPublishedHandles(OPERATOR, Set.of(pageId), "es");
        verify(experienceQuery).findPublishedHandles(OPERATOR, Set.of(experienceId), "es");
    }

    /** No menus means no lookups at all — nothing to resolve. */
    @Test
    void anOperatorWithNoMenusAsksNeitherContext() {
        shopExists("es", Set.of("es"));

        assertThat(useCase.execute("acme", null).orElseThrow().menus()).isEmpty();
        verify(pageQuery, never()).findPublishedHandles(any(), any(), anyString());
        verify(experienceQuery, never()).findPublishedHandles(any(), any(), anyString());
    }
}
