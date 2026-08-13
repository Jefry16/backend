package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontTourOperatorQuery;
import com.vointika.shared.port.StorefrontTourOperatorQuery.GateView;
import com.vointika.storefront.application.port.UnlockTokenPort;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

/**
 * Verifies a submitted password and mints the cookie value that unlocks the
 * store. Empty is a refusal, and every refusal looks the same — a wrong
 * password, an unknown tenant and a gate with no password set are one answer, so
 * the form tells a visitor nothing beyond "not this".
 */
public class UnlockStorefrontUseCase {

    private final StorefrontTourOperatorQuery storefrontShopQuery;
    private final UnlockTokenPort unlockToken;

    public UnlockStorefrontUseCase(StorefrontTourOperatorQuery storefrontShopQuery, UnlockTokenPort unlockToken) {
        this.storefrontShopQuery = storefrontShopQuery;
        this.unlockToken = unlockToken;
    }

    /** @return the value to put in the unlock cookie, or empty when refused */
    public Optional<String> execute(String handle, String submittedPassword) {
        return storefrontShopQuery.findGate(handle)
                .filter(GateView::passwordEnabled)
                .filter(gate -> accepts(gate, submittedPassword))
                .map(gate -> unlockToken.compute(gate.storefrontPassword(), gate.tourOperatorId()));
    }

    private static boolean accepts(GateView gate, String submittedPassword) {
        if (gate.storefrontPassword() == null || gate.storefrontPassword().isEmpty()
                || submittedPassword == null) {
            return false;
        }
        return MessageDigest.isEqual(
                submittedPassword.getBytes(StandardCharsets.UTF_8),
                gate.storefrontPassword().getBytes(StandardCharsets.UTF_8));
    }
}
