package com.vointika.identity.domain.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class RefreshToken {

    private static final int EXPIRATION_DAYS = 30;

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final UUID familyId;
    private UUID replacedById;
    private boolean revoked;
    private final Instant expiresAt;
    private final Instant createdAt;

    private RefreshToken(UUID id, UUID userId, String tokenHash, UUID familyId,
                         UUID replacedById, boolean revoked,
                         Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.replacedById = replacedById;
        this.revoked = revoked;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static RefreshToken newRoot(UUID id, UUID userId, String tokenHash) {
        Instant now = Instant.now();
        return new RefreshToken(id, userId, tokenHash, id, null, false,
                now.plus(Duration.ofDays(EXPIRATION_DAYS)), now);
    }

    /**
     * Builds the next token in {@code previous}'s family. Does NOT mutate
     * {@code previous}: revoking it (and stamping its {@code replaced_by_id}) is
     * done atomically in the DB via a guarded conditional update, so two
     * concurrent rotations of the same token can't both succeed — see
     * {@code RefreshTokenRepository.revokeForRotation}.
     */
    public static RefreshToken createRotation(RefreshToken previous, UUID newId, String newTokenHash) {
        Instant now = Instant.now();
        return new RefreshToken(newId, previous.userId, newTokenHash, previous.familyId, null, false,
                now.plus(Duration.ofDays(EXPIRATION_DAYS)), now);
    }

    public static RefreshToken rehydrate(UUID id, UUID userId, String tokenHash, UUID familyId,
                                         UUID replacedById, boolean revoked,
                                         Instant expiresAt, Instant createdAt) {
        return new RefreshToken(id, userId, tokenHash, familyId, replacedById, revoked, expiresAt, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public UUID getFamilyId() { return familyId; }
    public UUID getReplacedById() { return replacedById; }
    public boolean isRevoked() { return revoked; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
