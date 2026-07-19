package com.vointika.identity.domain.repository;

import com.vointika.identity.domain.entity.VerificationToken;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenRepository {
    VerificationToken save(VerificationToken token);
    Optional<VerificationToken> findByTokenHash(String tokenHash);
    void expireAllByUserId(UUID userId);
}
