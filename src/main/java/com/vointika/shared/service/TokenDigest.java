package com.vointika.shared.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The at-rest form of an opaque token: SHA-256 of its UTF-8 bytes, lower-case hex.
 *
 * <p>Two adapters computed this identically — identity's verification, password-reset
 * and refresh tokens, and {@code touroperator}'s invitation tokens, whose port javadoc
 * says it mirrors identity's posture deliberately. The *ports* stay separate, because
 * each context owns when it hashes and what it stores; only the arithmetic is shared,
 * so the two cannot drift into storing different digests of the same string.
 *
 * <p>No salt and no work factor, deliberately: these are 32 bytes of
 * {@code SecureRandom}, not passwords. A digest here defends against a leaked database
 * being replayable, and a fast hash is correct for a value with full entropy — unlike
 * {@code PasswordHasherPort}, which must be slow.
 */
public final class TokenDigest {

    private TokenDigest() {
    }

    public static String hexOf(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // Every JVM ships SHA-256; unreachable short of a broken runtime.
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
