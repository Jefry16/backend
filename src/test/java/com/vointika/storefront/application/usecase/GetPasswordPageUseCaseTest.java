package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.storefront.application.dto.output.PasswordPageOutput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetPasswordPageUseCaseTest {

    private static final UUID OPERATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private StorefrontShopQuery storefrontShopQuery;
    private GetPasswordPageUseCase useCase;

    @BeforeEach
    void setUp() {
        storefrontShopQuery = mock(StorefrontShopQuery.class);
        useCase = new GetPasswordPageUseCase(storefrontShopQuery);
    }

    /**
     * The content is asked for in the <b>primary</b> locale, never in one the
     * request chose — the gate answers before any locale is resolved.
     */
    @Test
    void rendersTheShopNameAndMessageInThePrimaryLocale() {
        when(storefrontShopQuery.findGate("acme")).thenReturn(Optional.of(new StorefrontGateView(
                OPERATOR, true, "hunter2", "Volvemos pronto", "es", Set.of("es", "en"))));
        when(storefrontShopQuery.findContent(OPERATOR, "es")).thenReturn(Optional.of(
                new StorefrontShopView("Acme Tours", "Calle Mayor 1", null, null,
                        null, null, "EUR", "€", "Europe/Madrid", "Madrid", null, null)));

        PasswordPageOutput page = useCase.execute("acme").orElseThrow();

        assertThat(page.shopName()).isEqualTo("Acme Tours");
        assertThat(page.message()).isEqualTo("Volvemos pronto");
        verify(storefrontShopQuery).findContent(OPERATOR, "es");
    }

    @Test
    void returnsNothingForAnUnknownHandle() {
        when(storefrontShopQuery.findGate("nope")).thenReturn(Optional.empty());

        assertThat(useCase.execute("nope")).isEmpty();
    }
}
