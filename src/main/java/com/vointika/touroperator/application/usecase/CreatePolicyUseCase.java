package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.input.CreatePolicyInput;
import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.PolicyBody;
import com.vointika.touroperator.domain.valueobject.PolicyTitle;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.exception.UniqueConstraintViolationException;
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
 * Writes a policy the operator has not written yet. ADMIN+.
 *
 * <p>The type comes from the body and is <b>immutable afterwards</b> — it is the
 * document's address on the storefront, so changing it is a delete and a create.
 * An operator can hold at most one of each type, which is a UNIQUE constraint
 * rather than a rule this class owns.
 *
 * <p>Guards: unknown type name → <b>422</b> (a body field, unlike a path segment
 * naming no resource); a type already written → <b>409</b>. The pre-check
 * answers in the use case's own terms and the constraint still decides, so a
 * lost race is translated rather than surfacing as a 500 (PATTERNS §8d).
 */
public class CreatePolicyUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;
    private final IdGenerator idGenerator;

    public CreatePolicyUseCase(TourOperatorRepository tourOperatorRepository,
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

    public UUID execute(UUID tourOperatorId, CreatePolicyInput input, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        PolicyType type = PolicyType.from(input.type() == null ? "" : input.type())
                .orElseThrow(() -> new InvalidFieldException(
                        "Unknown policy type: " + input.type()));

        if (tourOperatorRepository.findById(tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Tour operator not found");
        }
        if (policyRepository.existsByTourOperatorIdAndType(tourOperatorId, type)) {
            throw new ResourceAlreadyExistsException(
                    "A " + type.name() + " policy already exists");
        }

        PolicyTitle title = new PolicyTitle(input.title());
        PolicyBody body = new PolicyBody(input.body());
        UUID id = idGenerator.newId();
        Instant now = Instant.now();

        try {
            transactionRunner.run(() -> {
                policyRepository.save(Policy.write(id, tourOperatorId, type, title, body, now));
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "TOUR_OPERATOR", tourOperatorId, "tour_operator.policy_created",
                        Map.of("type", type.name())));
            });
        } catch (UniqueConstraintViolationException e) {
            // The pre-check above lost a race; the constraint is the real guard.
            throw new ResourceAlreadyExistsException(
                    "A " + type.name() + " policy already exists");
        }
        return id;
    }
}
