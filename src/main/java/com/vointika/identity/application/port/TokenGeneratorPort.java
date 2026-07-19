package com.vointika.identity.application.port;

public interface TokenGeneratorPort {
    String generateVerificationToken();
    String generatePasswordResetToken();
    String generateRefreshToken();
    String generateAccessToken(String userId);
}