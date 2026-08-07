package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
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

    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeletePolicyUseCase(TourOperatorPolicyRepository policyRepository,
                               TourOperatorMembershipCheck membershipCheck,
                               TransactionRunner transactionRunner,
                               AuditTrailPort auditTrailPort) {
        this.policyRepository = policyRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID policyId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        // Tenant-scoped probe: an id from another operator finds nothing, so this
        // is an idempotent no-op rather than a cross-tenant delete.
        var existing = policyRepository.findByIdAndTourOperatorId(policyId, tourOperatorId);
        if (existing.isEmpty()) {
            return;
        }
        transactionRunner.run(() -> {
            policyRepository.deleteById(policyId);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.policy_deleted",
                    Map.of("type", existing.get().type().name())));
        });
    }
}
