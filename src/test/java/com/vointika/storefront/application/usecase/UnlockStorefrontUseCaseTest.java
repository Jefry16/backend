package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontTourOperatorQuery;
import com.vointika.shared.port.StorefrontTourOperatorQuery.GateView;
import com.vointika.storefront.infrastructure.security.HmacUnlockToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UnlockStorefrontUseCaseTest {

    private static final UUID OPERATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    /** The real token adapter, so the minted value is checked as a browser would carry it. */
    private final HmacUnlockToken unlockToken = new HmacUnlockToken();

    private StorefrontTourOperatorQuery storefrontTourOperatorQuery;
    private UnlockStorefrontUseCase useCase;

    @BeforeEach
    void setUp() {
        storefrontTourOperatorQuery = mock(StorefrontTourOperatorQuery.class);
        useCase = new UnlockStorefrontUseCase(storefrontTourOperatorQuery, unlockToken);
    }

    @Test
    void theRightPasswordMintsACookieTheGateAccepts() {
        gate(true, "hunter2");

        String token = useCase.execute("acme", "hunter2").orElseThrow();

        assertThat(unlockToken.matches(token, "hunter2", OPERATOR)).isTrue();
    }

    @Test
    void theWrongPasswordIsRefused() {
        gate(true, "hunter2");

        assertThat(useCase.execute("acme", "hunter3")).isEmpty();
        assertThat(useCase.execute("acme", "")).isEmpty();
        assertThat(useCase.execute("acme", null)).isEmpty();
    }

    /** Case matters, and so does whitespace: the value is compared as stored. */
    @Test
    void thePasswordIsComparedVerbatim() {
        gate(true, "hunter2");

        assertThat(useCase.execute("acme", "HUNTER2")).isEmpty();
        assertThat(useCase.execute("acme", " hunter2")).isEmpty();
    }

    /**
     * The gate on with no password set has no way in — not even by submitting
     * nothing, which is what a null-tolerant comparison would have allowed.
     */
    @Test
    void theGateOnWithNoPasswordSetRefusesEverything() {
        gate(true, null);

        assertThat(useCase.execute("acme", null)).isEmpty();
        assertThat(useCase.execute("acme", "")).isEmpty();
        assertThat(useCase.execute("acme", "anything")).isEmpty();
    }

    @Test
    void aStoreWithTheGateOffMintsNothing() {
        gate(false, "hunter2");

        assertThat(useCase.execute("acme", "hunter2")).isEmpty();
    }

    @Test
    void anUnknownHandleIsRefusedLikeAWrongPassword() {
        when(storefrontTourOperatorQuery.findGate("nope")).thenReturn(Optional.empty());

        assertThat(useCase.execute("nope", "hunter2")).isEmpty();
    }

    private void gate(boolean passwordEnabled, String password) {
        when(storefrontTourOperatorQuery.findGate("acme")).thenReturn(Optional.of(new GateView(
                OPERATOR, "Acme Tours", passwordEnabled, password, null)));
    }
}
