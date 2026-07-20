package com.vointika.touroperator.domain.entity;

import com.vointika.shared.exception.ConflictException;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.valueobject.InviteeEmail;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TourOperatorInvitationTest {

    private TourOperatorInvitation withStatusAndExpiry(InvitationStatus status, Instant expiresAt) {
        Instant created = Instant.parse("2026-01-01T00:00:00Z");
        return new TourOperatorInvitation(
                UUID.randomUUID(), UUID.randomUUID(),
                new InviteeEmail("invitee@example.com"), MemberRole.STAFF,
                "hash", status, UUID.randomUUID(), created, expiresAt, null);
    }

    @Test
    void issueStartsPendingValidForSevenDays() {
        TourOperatorInvitation inv = TourOperatorInvitation.issue(
                UUID.randomUUID(), UUID.randomUUID(),
                new InviteeEmail("invitee@example.com"), MemberRole.ADMIN,
                "hash", UUID.randomUUID());

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
    void effectiveStatusReadsExpiredForAPendingRowPastExpiry() {
        TourOperatorInvitation inv = withStatusAndExpiry(
                InvitationStatus.PENDING, Instant.parse("2026-01-08T00:00:00Z"));

        Instant afterExpiry = Instant.parse("2026-02-01T00:00:00Z");
        assertEquals(InvitationStatus.EXPIRED, inv.effectiveStatus(afterExpiry));
        // Underlying status is untouched (lazy — no eager flip).
        assertEquals(InvitationStatus.PENDING, inv.getStatus());
    }
}
