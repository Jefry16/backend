package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.enums.MemberRole;

import java.util.Optional;
import java.util.UUID;

public interface TourOperatorMemberRepository {
    TourOperatorMember save(TourOperatorMember member);

    /** Whether the user is already a member of any operator — drives the isDefault flag. */
    boolean existsByUserId(UUID userId);

    /** Whether the user is a member of THIS operator (membership gate + already-member guard). */
    boolean existsByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId);

    /** The user's role in THIS operator, if a member (the role policy's tier check). */
    Optional<MemberRole> findRoleByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId);
}
