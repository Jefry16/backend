package com.vointika.shared.port;

public interface AccessTokenValidatorPort {
    boolean isValid(String token);
    String extractUserId(String token);
}