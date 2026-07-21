package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.InvitationView;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;

import java.time.Instant;
import java.util.UUID;

/**
 * A single invitation's admin detail. {@code id} is the invitation's id and
 * {@code context} is {@code "invitations"} (house convention: the id field is
 * always {@code id}, and {@code context} names the entity's collection).
 * {@code acceptedAt} is null unless the invitation was accepted.
 */
public record InvitationResponse(
        UUID id,
        String context,
        String email,
        MemberRole role,
        InvitationStatus status,
        boolean expired,
        Instant createdAt,
        Instant expiresAt,
        Instant acceptedAt) {

    public InvitationResponse(UUID id, String email, MemberRole role, InvitationStatus status,
                              boolean expired, Instant createdAt, Instant expiresAt, Instant acceptedAt) {
        this(id, "invitations", email, role, status, expired, createdAt, expiresAt, acceptedAt);
    }

    public static InvitationResponse from(InvitationView view) {
        return new InvitationResponse(
                view.id(), view.email(), view.role(), view.status(),
                view.expired(), view.createdAt(), view.expiresAt(), view.acceptedAt());
    }
}
