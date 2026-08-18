package com.vointika.touroperator.infrastructure.port;

import com.vointika.shared.service.TokenDigest;
import com.vointika.touroperator.application.port.InvitationTokenPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 32 random bytes, base64url without padding on the wire; SHA-256 hex (64 chars)
 * at rest — the same mechanics as identity's opaque tokens.
 */
@Component
public class InvitationTokenPortImpl implements InvitationTokenPort {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String hash(String rawToken) {
        return TokenDigest.hexOf(rawToken);
    }
}
