package com.vointika.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMs
) {

    /** HS256 requires a key of at least 256 bits; reject anything shorter. */
    private static final int MIN_SECRET_BYTES = 32;

    /**
     * Fail fast at startup on a missing or too-weak signing secret, rather than
     * lazily on the first token operation (a {@code WeakKeyException}, or an NPE
     * if the property is absent). Config precondition — {@link IllegalStateException}
     * per constitution §6.3.
     */
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("app.jwt.secret must be configured");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret must be at least " + MIN_SECRET_BYTES
                            + " bytes (256 bits) for HS256 signing");
        }
    }
}