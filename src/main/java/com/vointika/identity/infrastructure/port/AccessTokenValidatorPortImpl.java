package com.vointika.identity.infrastructure.port;

import com.vointika.shared.port.AccessTokenValidatorPort;
import com.vointika.identity.infrastructure.config.JwtProperties;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class AccessTokenValidatorPortImpl implements AccessTokenValidatorPort {

    private final SecretKey verificationKey;

    public AccessTokenValidatorPortImpl(JwtProperties jwtProperties) {
        this.verificationKey = Keys.hmacShaKeyFor(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean isValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public String extractUserId(String token) {
        return extractClaims(token).getSubject();
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(verificationKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}