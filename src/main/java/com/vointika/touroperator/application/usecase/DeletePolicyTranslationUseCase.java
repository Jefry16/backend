package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyTranslationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Map;
import java.util.UUID;

/**
 * Removes one policy's overlay for one locale, so that locale falls back to the
 * canonical document. ADMIN+, idempotent — deleting an absent overlay records
 * nothing.
 */
public class DeletePolicyTranslationUseCase {

    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorPolicyTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeletePolicyTranslationUseCase(TourOperatorPolicyRepository policyRepository,
                                          TourOperatorPolicyTranslationRepository translationRepository,
                                          TourOperatorMembershipCheck membershipCheck,
                                          TransactionRunner transactionRunner,
                                          AuditTrailPort auditTrailPort) {
        this.policyRepository = policyRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID policyId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        String locale = new LocaleCode(rawLocale).value();
        PolicyType type = policyRepository.findByIdAndTourOperatorId(policyId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"))
                .type();

        if (translationRepository.find(tourOperatorId, type, locale).isEmpty()) {
            return;
        }
        transactionRunner.run(() -> {
            translationRepository.delete(tourOperatorId, type, locale);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.policy_translation_deleted",
                    Map.of("type", type.name(), "locale", locale)));
        });
    }
}
