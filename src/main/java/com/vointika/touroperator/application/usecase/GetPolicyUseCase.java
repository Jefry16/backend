package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.output.PolicyView;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * One policy's canonical text, for the admin editor. Member-visible.
 *
 * <p>The lookup is <b>tenant-scoped</b>: an id belonging to another operator is
 * a 404, byte-identical to one that does not exist, so a policy id cannot be
 * used to probe another tenant.
 */
public class GetPolicyUseCase {

    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetPolicyUseCase(TourOperatorPolicyRepository policyRepository,
                            TourOperatorMembershipCheck membershipCheck) {
        this.policyRepository = policyRepository;
        this.membershipCheck = membershipCheck;
    }

    public PolicyView execute(UUID tourOperatorId, UUID policyId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return policyRepository.findByIdAndTourOperatorId(policyId, tourOperatorId)
                .map(PolicyView::from)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
    }
}
