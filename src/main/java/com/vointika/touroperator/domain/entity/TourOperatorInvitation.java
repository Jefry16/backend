package com.vointika.touroperator.domain.entity;

import com.vointika.shared.exception.ConflictException;
import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.valueobject.InviteeEmail;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * An invitation for an email address to join a tour operator's team. Keys on
 * EMAIL, not a user id — the invitee may not have an account yet; accepting
 * resolves/provisions the user and adds the {@link TourOperatorMember}.
 *
 * <p>The token is held HASHED (the raw token exists only in the emailed accept
 * link — identity's verification-token posture). Transitions are PENDING-only;
 * a current-state conflict raises {@link ConflictException} (409). Expiry is
 * judged lazily on access ({@link #isExpired}), never flipped by a job.
 */
public class TourOperatorInvitation {

    /** An invite link is valid for 7 days; resend renews. */
    public static final Duration VALIDITY = Duration.ofDays(7);

    private final UUID id;
    private final UUID tourOperatorId;
    private final InviteeEmail email;
    private final MemberRole role;
    private String tokenHash;
    private InvitationStatus status;
    private final UUID invitedByUserId;
    private final Instant createdAt;
    private Instant expiresAt;
    private Instant acceptedAt;

    /** Issues a brand-new PENDING invitation valid for {@link #VALIDITY}. */
    public static TourOperatorInvitation issue(UUID id,
                                               UUID tourOperatorId,
                                               InviteeEmail email,
                                               MemberRole role,
                                               String tokenHash,
                                               UUID invitedByUserId) {
        Instant now = Instant.now();
        return new TourOperatorInvitation(id, tourOperatorId, email, role, tokenHash,
                InvitationStatus.PENDING, invitedByUserId, now, now.plus(VALIDITY), null);
    }

    // Reconstitution from persistence.
    public TourOperatorInvitation(UUID id,
                                  UUID tourOperatorId,
                                  InviteeEmail email,
                                  MemberRole role,
                                  String tokenHash,
                                  InvitationStatus status,
                                  UUID invitedByUserId,
                                  Instant createdAt,
                                  Instant expiresAt,
                                  Instant acceptedAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.email = email;
        this.role = role;
        this.tokenHash = tokenHash;
        this.status = status;
        this.invitedByUserId = invitedByUserId;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.acceptedAt = acceptedAt;
    }

    /** Completes the invitation (PENDING → ACCEPTED). Callers own the surrounding gate. */
    public void accept() {
        requirePending("accepted");
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
    }

    /** Whether the accept link has lapsed (EXPIRED is judged on access). */
    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }

    private void requirePending(String verb) {
        if (status != InvitationStatus.PENDING) {
            throw new ConflictException("Only pending invitations can be " + verb);
        }
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public InviteeEmail getEmail() { return email; }
    public MemberRole getRole() { return role; }
    public String getTokenHash() { return tokenHash; }
    public InvitationStatus getStatus() { return status; }
    public UUID getInvitedByUserId() { return invitedByUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
}
