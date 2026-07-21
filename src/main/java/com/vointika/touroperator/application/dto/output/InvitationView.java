package com.vointika.touroperator.application.dto.output;

import com.vointika.touroperator.domain.entity.TourOperatorInvitation;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;

import java.time.Instant;
import java.util.UUID;

/**
 * The admin-facing detail of a single invitation. {@code status} is the raw
 * persisted lifecycle state; {@code expired} folds in the server's lazy-expiry
 * rule (a PENDING invitation past its {@code expiresAt}) so the caller doesn't
 * re-derive it — the same {@link TourOperatorInvitation#isExpired} the accept
 * path uses, the single source of truth.
 */
public record InvitationView(
        UUID id,
        String email,
        MemberRole role,
        InvitationStatus status,
        boolean expired,
        Instant createdAt,
        Instant expiresAt,
        Instant acceptedAt) {

    public static InvitationView from(TourOperatorInvitation invitation, Instant now) {
        return new InvitationView(
                invitation.getId(),
                invitation.getEmail().value(),
                invitation.getRole(),
                invitation.getStatus(),
                invitation.getStatus() == InvitationStatus.PENDING && invitation.isExpired(now),
                invitation.getCreatedAt(),
                invitation.getExpiresAt(),
                invitation.getAcceptedAt());
    }
}
