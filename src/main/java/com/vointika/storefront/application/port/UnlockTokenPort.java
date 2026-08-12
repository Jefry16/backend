package com.vointika.storefront.application.port;

import java.util.UUID;

/**
 * The value the unlock cookie carries, and the check that it is genuine.
 *
 * <p>The implementation is
 * {@code HMAC-SHA256(key = storefront password, message = operator id)}, hex.
 * Three properties decided it: it is <b>unforgeable</b> without knowing the
 * password; it needs <b>no new secret and no config</b>, since the password it
 * gates is the key; and <b>rotating the password invalidates every outstanding
 * cookie for free</b> — the old key stops producing the old digest, so there is
 * no session store to sweep and no revocation list to keep.
 *
 * <p><b>It is a port because {@code javax.crypto} is not {@code java.*}.</b> The
 * application layer's ArchUnit allowlist is {@code com.vointika..} plus
 * {@code java..} and has no exemptions, and {@code javax.crypto.Mac} matches
 * neither — verified by putting it in a use case and watching
 * {@code application_depends_only_on_our_code_and_the_jdk} fail. Same rule as
 * every third-party library: a use case that needs one is missing a port.
 */
public interface UnlockTokenPort {

    /**
     * Host-scoped by the browser, so {@code acme.…} and {@code beta.…} cannot
     * share one cookie and no cross-tenant unlock is possible. It lives here
     * because the interceptor that reads it and the controller that writes it are
     * in {@code infrastructure} and {@code presentation}, which may not see each
     * other.
     */
    String COOKIE_NAME = "storefront_unlock";

    String compute(String storefrontPassword, UUID tourOperatorId);

    /**
     * @param presented the cookie value a visitor sent, or {@code null} for none
     * @return whether it was minted from this operator's current password —
     *         {@code false} whenever there is no password to mint from, so a
     *         gate enabled with none set is locked with no way in
     */
    boolean matches(String presented, String storefrontPassword, UUID tourOperatorId);
}
