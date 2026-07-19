package com.vointika.identity.domain.entity;

import com.vointika.identity.domain.enums.TokenStatus;
import com.vointika.shared.exception.InvalidFieldException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class PasswordResetToken {

    private static final int EXPIRATION_HOURS = 1;

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private TokenStatus status;
    private final Instant expiresAt;
    private final Instant createdAt;

    private PasswordResetToken(UUID id, UUID userId, String tokenHash, TokenStatus status,
                               Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static PasswordResetToken issue(UUID id, UUID userId, String tokenHash) {
        Instant now = Instant.now();
        return new PasswordResetToken(id, userId, tokenHash, TokenStatus.PENDING,
                now.plus(Duration.ofHours(EXPIRATION_HOURS)), now);
    }

    public static PasswordResetToken rehydrate(UUID id, UUID userId, String tokenHash, TokenStatus status,
                                               Instant expiresAt, Instant createdAt) {
        return new PasswordResetToken(id, userId, tokenHash, status, expiresAt, createdAt);
    }

    public void use() {
        validate();
        this.status = TokenStatus.USED;
    }

    private void validate() {
        if (this.status == TokenStatus.USED) {
            throw new InvalidFieldException("Password reset token has already been used");
        }
        if (this.status == TokenStatus.EXPIRED || Instant.now().isAfter(this.expiresAt)) {
            throw new InvalidFieldException("Password reset token has expired");
        }
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public TokenStatus getStatus() { return status; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
