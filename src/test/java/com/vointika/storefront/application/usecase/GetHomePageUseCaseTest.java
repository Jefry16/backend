package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.storefront.application.dto.output.HomePageOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class GetHomePageUseCaseTest {

    private static final UUID OPERATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private StorefrontShopQuery storefrontShopQuery;
    private GetHomePageUseCase useCase;

    @BeforeEach
    void setUp() {
        storefrontShopQuery = mock(StorefrontShopQuery.class);
        useCase = new GetHomePageUseCase(storefrontShopQuery);
        when(storefrontShopQuery.findGate("acme")).thenReturn(Optional.of(new StorefrontGateView(
                OPERATOR, false, null, null, "es", Set.of("es", "en", "fr"))));
    }

    @Test
    void aBarePathRendersThePrimaryLocale() {
        content("es", new StorefrontShopView("Acme Tours", "logo.png", "og.png",
                "Acme Tours — excursiones", "Salidas en velero"));

        HomePageOutput page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.locale()).isEqualTo("es");
        assertThat(page.title()).isEqualTo("Acme Tours — excursiones");
        assertThat(page.shopName()).isEqualTo("Acme Tours");
        assertThat(page.description()).isEqualTo("Salidas en velero");
        assertThat(page.logoKey()).isEqualTo("logo.png");
        assertThat(page.ogImageKey()).isEqualTo("og.png");
    }

    @Test
    void aSupportedSecondaryRendersItsOwnContent() {
        content("en", new StorefrontShopView("Acme Tours", null, null, "Acme Tours — day trips", null));

        HomePageOutput page = useCase.execute("acme", "en").orElseThrow();

        assertThat(page.locale()).isEqualTo("en");
        assertThat(page.title()).isEqualTo("Acme Tours — day trips");
    }

    /** The primary lives at {@code /}; a second URL for it would be duplicate content. */
    @Test
    void thePrimaryUnderAPrefixIsNoPage() {
        assertThat(useCase.execute("acme", "es")).isEmpty();
        verify(storefrontShopQuery, never()).findContent(any(), anyString());
    }

    @Test
    void anUnsupportedLocaleIsNoPage() {
        assertThat(useCase.execute("acme", "de")).isEmpty();
        verify(storefrontShopQuery, never()).findContent(any(), anyString());
    }

    @Test
    void returnsNothingForAnUnknownHandle() {
        when(storefrontShopQuery.findGate("nope")).thenReturn(Optional.empty());

        assertThat(useCase.execute("nope", null)).isEmpty();
    }

    @Test
    void titleFallsBackToTheShopNameWhenNoSeoTitleIsSet() {
        content("es", new StorefrontShopView("Acme Tours", null, null, null, null));

        assertThat(useCase.execute("acme", null).orElseThrow().title()).isEqualTo("Acme Tours");
    }

    /** Absent media stay absent — nothing invents an empty string for the resolver. */
    @Test
    void nullMediaKeysPassThroughAsNull() {
        content("es", new StorefrontShopView("Acme Tours", null, null, null, null));

        HomePageOutput page = useCase.execute("acme", null).orElseThrow();

        assertThat(page.logoKey()).isNull();
        assertThat(page.ogImageKey()).isNull();
        assertThat(page.description()).isNull();
    }

    private void content(String locale, StorefrontShopView shop) {
        when(storefrontShopQuery.findContent(OPERATOR, locale)).thenReturn(Optional.of(shop));
    }
}
