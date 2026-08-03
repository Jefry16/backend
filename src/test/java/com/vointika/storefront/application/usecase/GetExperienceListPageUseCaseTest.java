package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontExperienceQuery.StorefrontExperienceCard;
import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.storefront.application.dto.output.ExperienceListPageOutput;
import com.vointika.storefront.application.dto.output.ExperienceListPageOutput.ExperienceCard;
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

class GetExperienceListPageUseCaseTest {

    private static final UUID OPERATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private StorefrontShopQuery storefrontShopQuery;
    private StorefrontExperienceQuery storefrontExperienceQuery;
    private GetExperienceListPageUseCase useCase;

    @BeforeEach
    void setUp() {
        storefrontShopQuery = mock(StorefrontShopQuery.class);
        storefrontExperienceQuery = mock(StorefrontExperienceQuery.class);
        useCase = new GetExperienceListPageUseCase(storefrontShopQuery, storefrontExperienceQuery);
        when(storefrontShopQuery.findGate("acme")).thenReturn(Optional.of(new StorefrontGateView(
                OPERATOR, false, null, null, "es", Set.of("es", "en", "fr"))));
        when(storefrontExperienceQuery.findPublished(any(), anyString())).thenReturn(List.of());
    }

    @Test
    void aBarePathRendersThePrimaryLocalesCardsUnderTheShopsOwnTitle() {
        content("es", new StorefrontShopView("Acme Tours", "logo.png", "og.png",
                "Acme Tours — excursiones", "Salidas en velero"));
        cards("es", new StorefrontExperienceCard("paseo-en-velero", "Paseo en velero", "Crucero dorado",
                "tour-operators/1/sunset.jpg", 150, true));

        ExperienceListPageOutput page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.locale()).isEqualTo("es");
        assertThat(page.title()).isEqualTo("Acme Tours — excursiones");
        assertThat(page.shopName()).isEqualTo("Acme Tours");
        assertThat(page.description()).isEqualTo("Salidas en velero");
        assertThat(page.logoKey()).isEqualTo("logo.png");
        assertThat(page.ogImageKey()).isEqualTo("og.png");
        assertThat(page.cards()).containsExactly(new ExperienceCard(
                "paseo-en-velero", "Paseo en velero", "Crucero dorado", "tour-operators/1/sunset.jpg", 150));
    }

    /** The locale the gate resolved is the locale the cards are asked for — one decision, used twice. */
    @Test
    void aSupportedSecondaryAsksForThatLocalesCards() {
        content("en", new StorefrontShopView("Acme Tours", null, null, null, null));
        cards("en", new StorefrontExperienceCard("sunset-sailing-tour", "Sunset Sailing Tour",
                "Golden-hour cruise", null, 150, true));

        ExperienceListPageOutput page = useCase.execute("acme", "en").orElseThrow();

        assertThat(page.locale()).isEqualTo("en");
        assertThat(page.cards()).extracting(ExperienceCard::name).containsExactly("Sunset Sailing Tour");
        verify(storefrontExperienceQuery).findPublished(OPERATOR, "en");
    }

    /**
     * An operator who has published nothing still has a storefront: this is a
     * page with no cards, and 404 would mean the shop disappears the day its last
     * experience is unpublished.
     */
    @Test
    void anOperatorWithNothingPublishedStillHasAPage() {
        content("es", new StorefrontShopView("Acme Tours", null, null, null, null));

        ExperienceListPageOutput page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.cards()).isEmpty();
        assertThat(page.shopName()).isEqualTo("Acme Tours");
    }

    /** The primary lives at the unprefixed route; a second URL for it would be duplicate content. */
    @Test
    void thePrimaryUnderAPrefixIsNoPage() {
        assertThat(useCase.execute("acme", "es")).isEmpty();
        verify(storefrontShopQuery, never()).findContent(any(), anyString());
        verify(storefrontExperienceQuery, never()).findPublished(any(), anyString());
    }

    @Test
    void anUnsupportedLocaleIsNoPage() {
        assertThat(useCase.execute("acme", "de")).isEmpty();
        verify(storefrontExperienceQuery, never()).findPublished(any(), anyString());
    }

    @Test
    void returnsNothingForAnUnknownHandle() {
        when(storefrontShopQuery.findGate("nope")).thenReturn(Optional.empty());

        assertThat(useCase.execute("nope", null)).isEmpty();
        verify(storefrontExperienceQuery, never()).findPublished(any(), anyString());
    }

    @Test
    void titleFallsBackToTheShopNameWhenNoSeoTitleIsSet() {
        content("es", new StorefrontShopView("Acme Tours", null, null, null, null));

        assertThat(useCase.execute("acme", null).orElseThrow().title()).isEqualTo("Acme Tours");
    }

    private void content(String locale, StorefrontShopView shop) {
        when(storefrontShopQuery.findContent(OPERATOR, locale)).thenReturn(Optional.of(shop));
    }

    private void cards(String locale, StorefrontExperienceCard... cards) {
        when(storefrontExperienceQuery.findPublished(OPERATOR, locale)).thenReturn(List.of(cards));
    }
}
