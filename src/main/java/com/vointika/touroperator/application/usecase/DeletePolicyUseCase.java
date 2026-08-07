package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.util.Map;
import java.util.UUID;

/**
 * Removes a policy, which is how one is unpublished: the row's absence is the
 * unpublished state, so this takes the page off the storefront (that URL then
 * 404s) rather than hiding it behind a flag. ADMIN+.
 *
 * <p>Idempotent — deleting a policy that was never written records nothing, the
 * shape {@code DeleteOperatorTranslationUseCase} set. Its translations go with
 * it through the composite foreign key's {@code ON DELETE CASCADE}.
 */
public class DeletePolicyUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeletePolicyUseCase(TourOperatorRepository tourOperatorRepository,
                               TourOperatorPolicyRepository policyRepository,
                               TourOperatorMembershipCheck membershipCheck,
                               TransactionRunner transactionRunner,
                               AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.policyRepository = policyRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, String rawType, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        PolicyType type = PolicyType.from(rawType)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        if (tourOperatorRepository.findById(tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Tour operator not found");
        }
        // Probe first: an idempotent delete that removes nothing records nothing.
        if (policyRepository.findByTourOperatorIdAndType(tourOperatorId, type).isEmpty()) {
            return;
        }
        transactionRunner.run(() -> {
            policyRepository.deleteByTourOperatorIdAndType(tourOperatorId, type);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.policy_deleted",
                    Map.of("type", type.name())));
        });
    }
}
