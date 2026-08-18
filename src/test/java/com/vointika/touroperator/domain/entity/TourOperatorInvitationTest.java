package com.vointika.touroperator.domain.entity;

import com.vointika.shared.exception.ConflictException;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.shared.valueobject.Email;
import com.vointika.touroperator.domain.valueobject.InviteeName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TourOperatorInvitationTest {

    private TourOperatorInvitation withStatusAndExpiry(InvitationStatus status, Instant expiresAt) {
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        return new TourOperatorInvitation(
                UUID.randomUUID(), UUID.randomUUID(),
                new Email("invitee@example.com"), new InviteeName("Test Invitee"), MemberRole.STAFF,
                "hash", status, UUID.randomUUID(), "Olive Inviter", created, expiresAt, null);
    }

    @Test
    void issueStartsPendingValidForSevenDays() {
        TourOperatorInvitation inv = TourOperatorInvitation.issue(
                UUID.randomUUID(), UUID.randomUUID(),
                new Email("invitee@example.com"), new InviteeName("Test Invitee"), MemberRole.ADMIN,
                "hash", UUID.randomUUID(), "Olive Inviter");

        assertEquals(InvitationStatus.PENDING, inv.getStatus());
        assertNull(inv.getAcceptedAt());
        assertEquals(inv.getCreatedAt().plus(Duration.ofDays(7)), inv.getExpiresAt());
    }

    @Test
    void acceptTransitionsToAcceptedAndStampsTime() {
        TourOperatorInvitation inv = withStatusAndExpiry(
                InvitationStatus.PENDING, Instant.parse("2030-01-01T00:00:00Z"));

        inv.accept();

        assertEquals(InvitationStatus.ACCEPTED, inv.getStatus());
        assertNotNull(inv.getAcceptedAt());
    }

    @Test
    void acceptOnANonPendingInvitationConflicts() {
        TourOperatorInvitation accepted = withStatusAndExpiry(
                InvitationStatus.ACCEPTED, Instant.parse("2030-01-01T00:00:00Z"));
        assertThrows(ConflictException.class, accepted::accept);
    }

    @Test
    void revokeTransitionsPendingToRevoked() {
        TourOperatorInvitation inv = withStatusAndExpiry(
                InvitationStatus.PENDING, Instant.parse("2030-01-01T00:00:00Z"));

        inv.revoke();

        assertEquals(InvitationStatus.REVOKED, inv.getStatus());
    }

    @Test
    void revokeOnANonPendingInvitationConflicts() {
        TourOperatorInvitation accepted = withStatusAndExpiry(
                InvitationStatus.ACCEPTED, Instant.parse("2030-01-01T00:00:00Z"));
        assertThrows(ConflictException.class, accepted::revoke);
    }

    @Test
    void renewSwapsTokenHashAndExtendsExpiryKeepingPending() {
        TourOperatorInvitation inv = withStatusAndExpiry(
                InvitationStatus.PENDING, Instant.parse("2020-01-01T00:00:00Z"));
        Instant before = Instant.now();

        inv.renew("fresh-hash");

        assertEquals(InvitationStatus.PENDING, inv.getStatus());
        assertEquals("fresh-hash", inv.getTokenHash());
        // expiry is refreshed to roughly now + VALIDITY (bounded, not exact — wall clock).
        Instant expected = before.plus(TourOperatorInvitation.VALIDITY);
        assertTrue(inv.getExpiresAt().isAfter(expected.minusSeconds(5)));
        assertTrue(inv.getExpiresAt().isBefore(expected.plusSeconds(5)));
    }

    @Test
    void renewOnALapsedButPendingInvitationSucceeds() {
        TourOperatorInvitation lapsed = withStatusAndExpiry(
                InvitationStatus.PENDING, Instant.parse("2020-01-01T00:00:00Z"));

        lapsed.renew("fresh-hash");

        assertEquals(InvitationStatus.PENDING, lapsed.getStatus());
        assertNotNull(lapsed.getExpiresAt());
    }

    @Test
    void renewOnARevokedInvitationConflicts() {
        TourOperatorInvitation revoked = withStatusAndExpiry(
                InvitationStatus.REVOKED, Instant.parse("2030-01-01T00:00:00Z"));
        assertThrows(ConflictException.class, () -> revoked.renew("fresh-hash"));
    }

}
