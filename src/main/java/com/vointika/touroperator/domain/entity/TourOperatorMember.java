package com.vointika.touroperator.domain.entity;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.touroperator.domain.enums.MemberRole;

import java.time.Instant;
import java.util.UUID;

/**
 * A user's membership in a tour operator. On create there is exactly one member
 * — the creator, as {@link MemberRole#OWNER}. {@code isDefault} marks this as
 * the user's default operator (the picker's landing choice): true for their
 * first operator, false thereafter.
 */
public class TourOperatorMember {

    private final UUID id;
    private final UUID tourOperatorId;
    private final UUID userId;
    // Mutable — a member's role changes (promote/demote/ownership transfer).
    // The owner-invariant guards live in the DB (partial unique index) + future
    // use cases; this entity guard is value-level only.
    private MemberRole role;
    private boolean isDefault;
    private final Instant joinedAt;

    // Constructor for creating a new membership
    public TourOperatorMember(UUID id, UUID tourOperatorId, UUID userId, MemberRole role, boolean isDefault) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.userId = userId;
        this.role = role;
        this.isDefault = isDefault;
        this.joinedAt = Instant.now();
    }

    // Constructor for reconstituting from persistence
    public TourOperatorMember(UUID id,
                              UUID tourOperatorId,
                              UUID userId,
                              MemberRole role,
                              boolean isDefault,
                              Instant joinedAt) {
        this.id = id;
        this.tourOperatorId = tourOperatorId;
        this.userId = userId;
        this.role = role;
        this.isDefault = isDefault;
        this.joinedAt = joinedAt;
    }

    /**
     * Changes the member's role. Value-level guard only (non-null); the trust
     * invariants (exactly one OWNER, no self-promote) are enforced by the DB
     * and the use cases that manage the team.
     */
    public void changeRole(MemberRole newRole) {
        if (newRole == null) {
            throw new InvalidFieldException("Member role is required");
        }
        this.role = newRole;
    }

    public UUID getId() { return id; }
    public UUID getTourOperatorId() { return tourOperatorId; }
    public UUID getUserId() { return userId; }
    public MemberRole getRole() { return role; }
    public boolean isDefault() { return isDefault; }
    public Instant getJoinedAt() { return joinedAt; }
}
