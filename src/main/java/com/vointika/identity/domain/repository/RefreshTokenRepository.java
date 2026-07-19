package com.vointika.identity.domain.repository;

import com.vointika.identity.domain.entity.RefreshToken;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken token);
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void revokeAllByUserId(UUID userId);
    void revokeAllByFamilyId(UUID familyId);

    /**
     * Revoke the token {@code id} for rotation, stamping {@code replacedById},
     * only if it is still live. Returns {@code true} if this call won (the token
     * was live and is now revoked), {@code false} if it had already been revoked
     * (a concurrent rotation won the race).
     */
    boolean revokeForRotation(UUID id, UUID replacedById);
}
