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
        String name,
        MemberRole role,
        InvitationStatus status,
        boolean expired,
        Instant createdAt,
        Instant expiresAt,
        Instant acceptedAt,
        InvitedBy invitedBy) {

    /**
     * The user who issued the invitation: {@code id} is that user's id and
     * {@code context} is {@code "users"} (house convention for a nested actor
     * reference). {@code name} is best-effort from identity — null if the
     * account can't be resolved.
     */
    public record InvitedBy(UUID id, String context, String name) {
        public InvitedBy(UUID id, String name) {
            this(id, "users", name);
        }
    }

    public InvitationResponse(UUID id, String email, String name, MemberRole role, InvitationStatus status,
                              boolean expired, Instant createdAt, Instant expiresAt, Instant acceptedAt,
                              InvitedBy invitedBy) {
        this(id, "invitations", email, name, role, status, expired, createdAt, expiresAt, acceptedAt, invitedBy);
    }

    public static InvitationResponse from(InvitationView view) {
        return new InvitationResponse(
                view.id(), view.email(), view.name(), view.role(), view.status(),
                view.expired(), view.createdAt(), view.expiresAt(), view.acceptedAt(),
                new InvitedBy(view.invitedByUserId(), view.invitedByName()));
    }
}
