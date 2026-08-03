package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.storefront.application.dto.output.HomePageOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GetHomePageUseCaseTest {

    private StorefrontShopQuery storefrontShopQuery;
    private GetHomePageUseCase useCase;

    @BeforeEach
    void setUp() {
        storefrontShopQuery = mock(StorefrontShopQuery.class);
        useCase = new GetHomePageUseCase(storefrontShopQuery);
    }

    @Test
    void returnsThePageForAKnownHandle() {
        when(storefrontShopQuery.findByHandle("acme")).thenReturn(Optional.of(new StorefrontShopView(
                "Acme Tours", "tour-operators/1/logo.png", "tour-operators/1/og.png",
                "Acme Tours — day trips", "Boat tours and day trips")));

        HomePageOutput page = useCase.execute("acme").orElseThrow();

        assertThat(page.title()).isEqualTo("Acme Tours — day trips");
        assertThat(page.shopName()).isEqualTo("Acme Tours");
        assertThat(page.description()).isEqualTo("Boat tours and day trips");
        assertThat(page.logoKey()).isEqualTo("tour-operators/1/logo.png");
        assertThat(page.ogImageKey()).isEqualTo("tour-operators/1/og.png");
    }

    @Test
    void returnsNothingForAnUnknownHandle() {
        when(storefrontShopQuery.findByHandle("nope")).thenReturn(Optional.empty());

        assertThat(useCase.execute("nope")).isEmpty();
    }

    @Test
    void titleFallsBackToTheShopNameWhenNoSeoTitleIsSet() {
        when(storefrontShopQuery.findByHandle("acme")).thenReturn(Optional.of(
                new StorefrontShopView("Acme Tours", null, null, null, null)));

        assertThat(useCase.execute("acme").orElseThrow().title()).isEqualTo("Acme Tours");
    }

    /** Absent media stay absent — nothing invents an empty string for the resolver. */
    @Test
    void nullMediaKeysPassThroughAsNull() {
        when(storefrontShopQuery.findByHandle("acme")).thenReturn(Optional.of(
                new StorefrontShopView("Acme Tours", null, null, null, null)));

        HomePageOutput page = useCase.execute("acme").orElseThrow();

        assertThat(page.logoKey()).isNull();
        assertThat(page.ogImageKey()).isNull();
        assertThat(page.description()).isNull();
    }
}
