package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.TourOperatorMember;

import java.util.UUID;

public interface TourOperatorMemberRepository {
    TourOperatorMember save(TourOperatorMember member);

    /** Whether the user is already a member of any operator — drives the isDefault flag. */
    boolean existsByUserId(UUID userId);
}
