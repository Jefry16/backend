package com.vointika.touroperator.application.policy;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;

import java.util.UUID;

/**
 * The {@code touroperator} adapter for the shared membership port — the one place
 * role tiers are mapped. The shared port exposes only capability checks; this
 * resolves the caller's {@link MemberRole} and compares tiers, so {@code MemberRole}
 * never leaks into {@code shared} (PATTERNS §6).
 */
public class TourOperatorMembershipPolicy implements TourOperatorMembershipCheck {

    private final TourOperatorMemberRepository memberRepository;

    public TourOperatorMembershipPolicy(TourOperatorMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public void ensureMember(UUID userId, UUID tourOperatorId) {
        if (userId == null || tourOperatorId == null
                || !memberRepository.existsByTourOperatorIdAndUserId(tourOperatorId, userId)) {
            throw new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND);
        }
    }

    @Override
    public void ensureAdmin(UUID userId, UUID tourOperatorId) {
        requireAtLeast(userId, tourOperatorId, MemberRole.ADMIN);
    }

    @Override
    public void ensureOwner(UUID userId, UUID tourOperatorId) {
        requireAtLeast(userId, tourOperatorId, MemberRole.OWNER);
    }

    /**
     * Resolves the caller's role and enforces the minimum tier. An unresolvable
     * role (shouldn't happen once the interceptor has run, but never trust that)
     * is insufficient → 403, never a leak of tenant existence.
     */
    private void requireAtLeast(UUID userId, UUID tourOperatorId, MemberRole minimum) {
        MemberRole role = (userId == null || tourOperatorId == null)
                ? null
                : memberRepository.findRoleByTourOperatorIdAndUserId(tourOperatorId, userId)
                        .orElse(null);
        if (role == null || !role.atLeast(minimum)) {
            throw new ForbiddenException("This action requires " + minimum + " privileges");
        }
    }
}
