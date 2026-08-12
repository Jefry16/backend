package com.vointika.storefront.infrastructure.security;

import com.vointika.storefront.application.port.UnlockTokenPort;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * HMAC-SHA256 over the operator id, keyed by the storefront password.
 *
 * <p>Keying on the operator id means a cookie minted for one tenant does not
 * verify against another even if the two share a password. Cookies are
 * host-scoped as well, so that is belt and braces.
 */
@Component
public class HmacUnlockToken implements UnlockTokenPort {

    private static final String ALGORITHM = "HmacSHA256";

    @Override
    public String compute(String storefrontPassword, UUID tourOperatorId) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(storefrontPassword.getBytes(StandardCharsets.UTF_8), ALGORITHM));
            return HexFormat.of().formatHex(
                    mac.doFinal(tourOperatorId.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            // Every Java SE implementation is required to provide HmacSHA256, so
            // this is a broken JRE rather than a runtime condition to handle.
            throw new IllegalStateException("HmacSHA256 is unavailable", e);
        }
    }

    /**
     * <b>{@code MessageDigest.isEqual}, never {@code String.equals}</b>:
     * {@code equals} short-circuits on the first differing byte, which leaks how
     * much of a forged token is right and turns guessing into a per-byte search
     * rather than a search of the whole space.
     */
    @Override
    public boolean matches(String presented, String storefrontPassword, UUID tourOperatorId) {
        if (presented == null || storefrontPassword == null || storefrontPassword.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(
                presented.getBytes(StandardCharsets.UTF_8),
                compute(storefrontPassword, tourOperatorId).getBytes(StandardCharsets.UTF_8));
    }
}
