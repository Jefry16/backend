package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.output.PolicyView;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * One policy's canonical text, for the admin editor. Member-visible.
 *
 * <p><b>An unwritten policy is a 404</b>, unlike an untranslated locale, which
 * returns an empty overlay. The difference is real: a translation always exists
 * conceptually (the operator's text, in that language), while a policy either
 * has been written or has not — and its absence is what the storefront serves as
 * a 404. The editor renders a blank form from the closed type set, not from a
 * blank response.
 */
public class GetPolicyUseCase {

    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetPolicyUseCase(TourOperatorPolicyRepository policyRepository,
                            TourOperatorMembershipCheck membershipCheck) {
        this.policyRepository = policyRepository;
        this.membershipCheck = membershipCheck;
    }

    public PolicyView execute(UUID tourOperatorId, String rawType, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        PolicyType type = PolicyType.from(rawType)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
        return policyRepository.findByTourOperatorIdAndType(tourOperatorId, type)
                .map(PolicyView::from)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
    }
}
