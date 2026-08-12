package com.vointika.touroperator.application.port;

/**
 * Mints the storefront password a brand-new operator starts behind.
 *
 * <p><b>A port because randomness is not {@code java.*}-only in spirit and not
 * testable in place.</b> `SecureRandom` would pass the application layer's
 * allowlist, but a use case that draws its own randomness cannot be asserted
 * against a known value — this is the same shape as {@code InvitationTokenPort}
 * and identity's {@code TokenGeneratorPort}, for the same reason.
 *
 * <p>It is <b>not</b> a credential: the operator reads it back in admin and hands
 * it to whoever should see the store early. It is stored in plaintext by design
 * (Shopify's model), which is exactly why it is generated rather than chosen —
 * nobody should reuse a password here.
 */
public interface StorefrontPasswordGeneratorPort {

    String generate();
}
