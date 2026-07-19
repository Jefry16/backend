package com.vointika.identity.domain.entity;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void newRootTokenSetsFamilyIdToOwnId() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RefreshToken token = RefreshToken.newRoot(id, userId, "hash");

        assertEquals(id, token.getId());
        assertEquals(userId, token.getUserId());
        assertEquals("hash", token.getTokenHash());
        assertEquals(id, token.getFamilyId());
        assertNull(token.getReplacedById());
        assertFalse(token.isRevoked());
    }

    @Test
    void newRootExpires30DaysFromCreation() {
        RefreshToken token = RefreshToken.newRoot(UUID.randomUUID(), UUID.randomUUID(), "hash");
        assertTrue(token.getExpiresAt().isAfter(token.getCreatedAt().plus(Duration.ofDays(29))));
        assertTrue(token.getExpiresAt().isBefore(token.getCreatedAt().plus(Duration.ofDays(31))));
    }

    @Test
    void createRotationCarriesFamilyAndDoesNotMutatePrevious() {
        RefreshToken root = RefreshToken.newRoot(UUID.randomUUID(), UUID.randomUUID(), "hash-1");
        UUID newId = UUID.randomUUID();

        RefreshToken rotated = RefreshToken.createRotation(root, newId, "hash-2");

        assertEquals(root.getFamilyId(), rotated.getFamilyId());
        assertEquals(newId, rotated.getId());
        assertEquals("hash-2", rotated.getTokenHash());
        assertNull(rotated.getReplacedById());
        assertFalse(rotated.isRevoked());
        // previous is NOT mutated here — revoking it (and stamping replaced_by_id)
        // is done atomically in the DB via the guarded conditional update, so two
        // concurrent rotations can't both win.
        assertFalse(root.isRevoked());
        assertNull(root.getReplacedById());
    }
}
