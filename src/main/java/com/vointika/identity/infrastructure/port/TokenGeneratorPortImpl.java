package com.vointika.identity.infrastructure.port;

import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.infrastructure.config.JwtProperties;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;


@Component
public class TokenGeneratorPortImpl implements TokenGeneratorPort {

    private static final int OPAQUE_TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public TokenGeneratorPortImpl(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String generateVerificationToken() {
        return randomOpaqueToken();
    }

    @Override
    public String generatePasswordResetToken() {
        return randomOpaqueToken();
    }

    @Override
    public String generateRefreshToken() {
        return randomOpaqueToken();
    }

    private String randomOpaqueToken() {
        byte[] bytes = new byte[OPAQUE_TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    @Override
    public String generateAccessToken(String userId) {
        return Jwts.builder()
                .subject(userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.accessTokenExpirationMs()))
                .signWith(signingKey)
                .compact();
    }
}
