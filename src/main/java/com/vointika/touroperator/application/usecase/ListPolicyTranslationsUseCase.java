package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.output.PolicyTranslationView;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyTranslationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.UUID;

/**
 * Every locale one policy has been translated into. Member-visible.
 *
 * <p>Bounded by the operator's supported locales, so a plain list for the same
 * reason {@code ListPoliciesUseCase} is one.
 */
public class ListPolicyTranslationsUseCase {

    private final TourOperatorPolicyTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListPolicyTranslationsUseCase(TourOperatorPolicyTranslationRepository translationRepository,
                                         TourOperatorMembershipCheck membershipCheck) {
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public List<PolicyTranslationView> execute(UUID tourOperatorId, String rawType, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        PolicyType type = PolicyType.from(rawType)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
        return translationRepository.findAllByTourOperatorIdAndType(tourOperatorId, type).stream()
                .map(PolicyTranslationView::from)
                .toList();
    }
}
