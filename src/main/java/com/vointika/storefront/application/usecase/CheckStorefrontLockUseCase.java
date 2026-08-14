package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontTourOperatorQuery;
import com.vointika.shared.port.StorefrontTourOperatorQuery.GateView;
import com.vointika.storefront.application.port.UnlockTokenPort;

/**
 * Whether a storefront will answer this visitor at all — asked once, before any
 * locale exists, for every storefront path.
 *
 * <p>Ordering is the whole point. Resolving the locale first would make a locked
 * store answer {@code /es} with a 404 and {@code /fr} with a redirect, which
 * tells an anonymous visitor that the store exists and which locales it
 * publishes, from behind the gate. Locked means every path answers identically.
 */
public class CheckStorefrontLockUseCase {

    public enum LockState { NO_SUCH_TENANT, UNLOCKED, LOCKED }

    private final StorefrontTourOperatorQuery storefrontTourOperatorQuery;
    private final UnlockTokenPort unlockToken;

    public CheckStorefrontLockUseCase(StorefrontTourOperatorQuery storefrontTourOperatorQuery, UnlockTokenPort unlockToken) {
        this.storefrontTourOperatorQuery = storefrontTourOperatorQuery;
        this.unlockToken = unlockToken;
    }

    /**
     * @param presentedToken the unlock cookie's value, or {@code null} when the
     *                       visitor presented none
     */
    public LockState execute(String handle, String presentedToken) {
        return storefrontTourOperatorQuery.findGate(handle)
                .map(gate -> state(gate, presentedToken))
                .orElse(LockState.NO_SUCH_TENANT);
    }

    private LockState state(GateView gate, String presentedToken) {
        if (!gate.passwordEnabled()) {
            return LockState.UNLOCKED;
        }
        // The gate on with no password set is locked with no way in, never open —
        // treating it as unlocked would open a store by leaving a field blank,
        // which is why the port refuses rather than comparing null to null.
        // The admin API will not produce that row (enabling with no password is a
        // 422, checked against the running stack), so this is defence in depth
        // against a direct write or a future path that forgets the guard, not a
        // live case.
        return unlockToken.matches(presentedToken, gate.storefrontPassword(), gate.tourOperatorId())
                ? LockState.UNLOCKED
                : LockState.LOCKED;
    }
}
