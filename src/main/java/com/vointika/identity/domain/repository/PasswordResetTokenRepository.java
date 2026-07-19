package com.vointika.identity.domain.repository;

import com.vointika.identity.domain.entity.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    PasswordResetToken save(PasswordResetToken token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    void expireAllByUserId(UUID userId);
}
