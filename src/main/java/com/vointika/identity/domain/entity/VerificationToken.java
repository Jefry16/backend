package com.vointika.identity.domain.entity;

import com.vointika.identity.domain.enums.TokenStatus;
import com.vointika.shared.exception.InvalidFieldException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class VerificationToken {

    private static final int EXPIRATION_HOURS = 24;

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private TokenStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;

    private VerificationToken(UUID id, UUID userId, String tokenHash, TokenStatus status,
                              Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static VerificationToken issue(UUID id, UUID userId, String tokenHash) {
        Instant now = Instant.now();
        return new VerificationToken(id, userId, tokenHash, TokenStatus.PENDING,
                now.plus(Duration.ofHours(EXPIRATION_HOURS)), now);
    }

    public static VerificationToken rehydrate(UUID id, UUID userId, String tokenHash, TokenStatus status,
                                              Instant expiresAt, Instant createdAt) {
        return new VerificationToken(id, userId, tokenHash, status, expiresAt, createdAt);
    }

    public void use() {
        validate();
        this.status = TokenStatus.USED;
    }

    private void validate() {
        if (this.status == TokenStatus.USED) {
            throw new InvalidFieldException("Verification token has already been used");
        }
        if (this.status == TokenStatus.EXPIRED || Instant.now().isAfter(this.expiresAt)) {
            throw new InvalidFieldException("Verification token has expired");
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public TokenStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
