package com.vointika.touroperator.domain.repository;

import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;

import java.util.Optional;
import java.util.UUID;

public interface TourOperatorMemberRepository {
    TourOperatorMember save(TourOperatorMember member);

    /** Cursor-paginated roster (shared list framework), tenant-scoped to the operator. */
    CursorPage<TourOperatorMember> list(ListQuery query);

    /** Whether the user is already a member of any operator — drives the isDefault flag. */
    boolean existsByUserId(UUID userId);

    /** Whether the user is a member of THIS operator (membership gate + already-member guard). */
    boolean existsByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId);

    /** The user's role in THIS operator, if a member (the role policy's tier check). */
    Optional<MemberRole> findRoleByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId);

    /** A single membership by (operator, user) — owner-invariant reasoning on a role change. */
    Optional<TourOperatorMember> findByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId);

    /** How many members hold a given role — the last-OWNER guard counts on {@code OWNER}. */
    long countByTourOperatorIdAndRole(UUID tourOperatorId, MemberRole role);

    /** Removes one membership (self-leave / remove-member); authorization is the use case's job. */
    void deleteByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId);

    /**
     * Persists an ownership transfer, flushing the demotion before the promotion so
     * the single-owner partial unique index never momentarily sees two OWNERs.
     * {@code demotedOwner} must already be re-roled to ADMIN and {@code promotedMember}
     * to OWNER by the caller.
     */
    void transferOwnership(TourOperatorMember demotedOwner, TourOperatorMember promotedMember);
}
