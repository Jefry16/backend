package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.GateView;
import com.vointika.storefront.infrastructure.security.HmacUnlockToken;
import com.vointika.storefront.application.usecase.CheckStorefrontLockUseCase.LockState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CheckStorefrontLockUseCaseTest {

    private static final UUID OPERATOR = UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    /**
     * The real token adapter rather than a mock: it is deterministic and does no
     * I/O, and "a valid cookie unlocks, a tampered one does not" is only worth
     * asserting against the digest a browser would actually be carrying.
     */
    private final HmacUnlockToken unlockToken = new HmacUnlockToken();

    private StorefrontShopQuery storefrontShopQuery;
    private CheckStorefrontLockUseCase useCase;

    @BeforeEach
    void setUp() {
        storefrontShopQuery = mock(StorefrontShopQuery.class);
        useCase = new CheckStorefrontLockUseCase(storefrontShopQuery, unlockToken);
    }

    @Test
    void aStoreWithTheGateOffIsUnlockedForEveryone() {
        gate(false, null);

        assertThat(useCase.execute("acme", null)).isEqualTo(LockState.UNLOCKED);
    }

    @Test
    void aLockedStoreWithNoCookieIsLocked() {
        gate(true, "hunter2");

        assertThat(useCase.execute("acme", null)).isEqualTo(LockState.LOCKED);
    }

    @Test
    void aLockedStoreWithAValidCookieIsUnlocked() {
        gate(true, "hunter2");

        assertThat(useCase.execute("acme", unlockToken.compute("hunter2", OPERATOR)))
                .isEqualTo(LockState.UNLOCKED);
    }

    @Test
    void aTamperedCookieDoesNotUnlock() {
        gate(true, "hunter2");
        String tampered = unlockToken.compute("hunter2", OPERATOR).replace('a', 'b');

        assertThat(useCase.execute("acme", tampered)).isEqualTo(LockState.LOCKED);
    }

    /**
     * {@code password_enabled = true} with no password set is reachable through
     * the admin API. It is <b>locked with no way in</b> — reading it as unlocked
     * would open a store by leaving a field blank, and a null password must never
     * match a missing cookie.
     */
    @Test
    void theGateOnWithNoPasswordSetIsLockedWithNoWayIn() {
        gate(true, null);

        assertThat(useCase.execute("acme", null)).isEqualTo(LockState.LOCKED);
        assertThat(useCase.execute("acme", "")).isEqualTo(LockState.LOCKED);
        assertThat(useCase.execute("acme", unlockToken.compute("hunter2", OPERATOR)))
                .isEqualTo(LockState.LOCKED);
    }

    @Test
    void anUnknownHandleIsNoSuchTenant() {
        when(storefrontShopQuery.findGate("nope")).thenReturn(Optional.empty());

        assertThat(useCase.execute("nope", null)).isEqualTo(LockState.NO_SUCH_TENANT);
    }

    private void gate(boolean passwordEnabled, String password) {
        when(storefrontShopQuery.findGate("acme")).thenReturn(Optional.of(new GateView(
                OPERATOR, "Acme Tours", passwordEnabled, password, null)));
    }
}
