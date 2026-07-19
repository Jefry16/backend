package com.vointika.identity.domain.entity;

import com.vointika.identity.domain.enums.TokenStatus;
import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenTest {

    @Test
    void issueCreatesPendingTokenWithHash() {
        UUID userId = UUID.randomUUID();
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), userId, "hash");
        assertNotNull(token.getId());
        assertEquals(userId, token.getUserId());
        assertEquals("hash", token.getTokenHash());
        assertEquals(TokenStatus.PENDING, token.getStatus());
    }

    @Test
    void issueExpires1HourFromCreation() {
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash");
        assertTrue(token.getExpiresAt().isAfter(token.getCreatedAt().plus(Duration.ofMinutes(59))));
        assertTrue(token.getExpiresAt().isBefore(token.getCreatedAt().plus(Duration.ofMinutes(61))));
    }

    @Test
    void useMarksTokenUsed() {
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash");
        token.use();
        assertEquals(TokenStatus.USED, token.getStatus());
    }

    @Test
    void useTwiceThrows() {
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash");
        token.use();
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, token::use);
        assertEquals("Password reset token has already been used", ex.getMessage());
    }

    @Test
    void useExpiredTokenThrows() {
        PasswordResetToken token = PasswordResetToken.rehydrate(
                UUID.randomUUID(), UUID.randomUUID(), "hash", TokenStatus.PENDING,
                Instant.now().minus(Duration.ofMinutes(1)),
                Instant.now().minus(Duration.ofHours(2)));
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, token::use);
        assertEquals("Password reset token has expired", ex.getMessage());
        // Status is NOT flipped in memory — EXPIRED rows come only from the
        // JPQL bulk expires; the throw alone carries the outcome.
        assertEquals(TokenStatus.PENDING, token.getStatus());
    }
}
