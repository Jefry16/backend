package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.input.UpsertPolicyInput;
import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.PolicyBody;
import com.vointika.touroperator.domain.valueobject.PolicyTitle;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Writes one of the operator's four store policies, creating it or replacing its
 * text. ADMIN+; membership enforced by the interceptor.
 *
 * <p>Guards: caller not ADMIN+ → 403; operator missing → 404; a name no
 * {@link PolicyType} is called → 404, never {@code valueOf}'s
 * {@code IllegalArgumentException}, which would be a 500.
 *
 * <p><b>The audit entry carries the type, never the body.</b> A policy document
 * is up to 256 KiB of HTML and the trail is not a revision store — the row is
 * the current text and this records who changed it, the same shape the contact
 * context uses when it audits a deletion by summary alone.
 */
public class UpsertPolicyUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;
    private final IdGenerator idGenerator;

    public UpsertPolicyUseCase(TourOperatorRepository tourOperatorRepository,
                               TourOperatorPolicyRepository policyRepository,
                               TourOperatorMembershipCheck membershipCheck,
                               TransactionRunner transactionRunner,
                               AuditTrailPort auditTrailPort,
                               IdGenerator idGenerator) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.policyRepository = policyRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
        this.idGenerator = idGenerator;
    }

    public void execute(UUID tourOperatorId, String rawType,
                        UpsertPolicyInput input, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        PolicyType type = PolicyType.from(rawType)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));

        if (tourOperatorRepository.findById(tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Tour operator not found");
        }

        PolicyTitle title = new PolicyTitle(input.title());
        PolicyBody body = new PolicyBody(input.body());
        Instant now = Instant.now();

        transactionRunner.run(() -> {
            Policy policy = policyRepository.findByTourOperatorIdAndType(tourOperatorId, type)
                    .map(existing -> existing.rewrite(title, body, now))
                    .orElseGet(() -> Policy.write(idGenerator.newId(), tourOperatorId, type, title, body, now));
            policyRepository.upsert(policy);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.policy_updated",
                    Map.of("type", type.name())));
        });
    }
}
