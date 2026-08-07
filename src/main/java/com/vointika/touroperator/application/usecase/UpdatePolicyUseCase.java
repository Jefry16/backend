package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.input.UpdatePolicyInput;
import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.domain.valueobject.PolicyBody;
import com.vointika.touroperator.domain.valueobject.PolicyTitle;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Replaces a policy's text. ADMIN+. The type is not settable here — it is the
 * document's address, so moving it is a delete and a create.
 *
 * <p>The lookup is tenant-scoped, so a policy id belonging to another operator
 * is a 404 rather than a document.
 */
public class UpdatePolicyUseCase {

    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdatePolicyUseCase(TourOperatorPolicyRepository policyRepository,
                               TourOperatorMembershipCheck membershipCheck,
                               TransactionRunner transactionRunner,
                               AuditTrailPort auditTrailPort) {
        this.policyRepository = policyRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID policyId,
                        UpdatePolicyInput input, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Policy existing = policyRepository.findByIdAndTourOperatorId(policyId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        PolicyTitle title = new PolicyTitle(input.title());
        PolicyBody body = new PolicyBody(input.body());

        transactionRunner.run(() -> {
            policyRepository.save(existing.rewrite(title, body, Instant.now()));
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.policy_updated",
                    Map.of("type", existing.type().name())));
        });
    }
}
