package com.vointika.identity.domain.entity;

import com.vointika.identity.domain.enums.TokenStatus;
import com.vointika.shared.exception.InvalidFieldException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class VerificationTokenTest {

    @Test
    void issueCreatesPendingTokenWithHash() {
        UUID userId = UUID.randomUUID();
        VerificationToken token = VerificationToken.issue(UUID.randomUUID(), userId, "hash");
        assertNotNull(token.getId());
        assertEquals(userId, token.getUserId());
        assertEquals("hash", token.getTokenHash());
        assertEquals(TokenStatus.PENDING, token.getStatus());
    }

    @Test
    void issueExpires24HoursFromCreation() {
        VerificationToken token = VerificationToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash");
        assertTrue(token.getExpiresAt().isAfter(token.getCreatedAt().plus(Duration.ofHours(23))));
        assertTrue(token.getExpiresAt().isBefore(token.getCreatedAt().plus(Duration.ofHours(25))));
    }

    @Test
    void useMarksTokenUsed() {
        VerificationToken token = VerificationToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash");
        token.use();
        assertEquals(TokenStatus.USED, token.getStatus());
    }

    @Test
    void useTwiceThrows() {
        VerificationToken token = VerificationToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash");
        token.use();
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, token::use);
        assertEquals("Verification token has already been used", ex.getMessage());
    }

    @Test
    void useExpiredTokenThrows() {
        VerificationToken token = VerificationToken.rehydrate(
                UUID.randomUUID(), UUID.randomUUID(), "hash", TokenStatus.PENDING,
                Instant.now().minus(Duration.ofHours(1)),
                Instant.now().minus(Duration.ofDays(2)));
        InvalidFieldException ex = assertThrows(InvalidFieldException.class, token::use);
        assertEquals("Verification token has expired", ex.getMessage());
        // Status is NOT flipped in memory — EXPIRED rows come only from the
        // JPQL bulk expires; the throw alone carries the outcome.
        assertEquals(TokenStatus.PENDING, token.getStatus());
    }
}
