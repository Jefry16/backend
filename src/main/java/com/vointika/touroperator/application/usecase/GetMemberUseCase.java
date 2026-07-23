package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.MemberListView;
import com.vointika.touroperator.domain.entity.TourOperatorMember;
import com.vointika.touroperator.domain.repository.TourOperatorMemberRepository;

import java.util.UUID;

/**
 * Reads a single team member. Visible to ANY member of the operator (read-only,
 * mirroring the roster; the mutating actions change-role / remove stay ADMIN+).
 * Membership is enforced by the route interceptor and re-asserted here
 * (defense-in-depth).
 *
 * <p>Guards: caller not a member → 404 (indistinguishable from a missing
 * operator, tenant isolation); the user isn't a member of this operator → 404.
 * Returns the same shape as a roster row (name/email denormalized on the row).
 */
public class GetMemberUseCase {

    private final TourOperatorMemberRepository memberRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetMemberUseCase(TourOperatorMemberRepository memberRepository,
                            TourOperatorMembershipCheck membershipCheck) {
        this.memberRepository = memberRepository;
        this.membershipCheck = membershipCheck;
    }

    public MemberListView execute(UUID tourOperatorId, UUID userId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        TourOperatorMember member = memberRepository
                .findByTourOperatorIdAndUserId(tourOperatorId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        return new MemberListView(
                member.getUserId(),
                member.getRole(),
                member.getJoinedAt(),
                member.getName(),
                member.getEmail());
    }
}
