package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.output.PolicyView;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.UUID;

/**
 * Every policy the operator has written. Member-visible.
 *
 * <p><b>A plain list, deliberately.</b> PATTERNS §4b requires the cursor
 * framework for anything over tenant or growable data; this is neither — the
 * enum bounds it at four rows forever, the same exemption the reference lists
 * carry. A cursor here would be ceremony over a set that cannot grow.
 */
public class ListPoliciesUseCase {

    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListPoliciesUseCase(TourOperatorPolicyRepository policyRepository,
                               TourOperatorMembershipCheck membershipCheck) {
        this.policyRepository = policyRepository;
        this.membershipCheck = membershipCheck;
    }

    public List<PolicyView> execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return policyRepository.findAllByTourOperatorId(tourOperatorId).stream()
                .map(PolicyView::from)
                .toList();
    }
}
