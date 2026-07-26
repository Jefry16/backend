package com.vointika.audience.application.usecase;

import com.vointika.audience.application.dto.input.AudienceInput;
import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.audience.domain.valueobject.PaxPerUnit;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

/**
 * Creates an audience. ADMIN+ only; membership enforced by the route interceptor.
 * Names are unique per operator — a duplicate is rejected 409, both up-front and
 * on the unique-index race (never a 500).
 */
public class CreateAudienceUseCase {

    private final AudienceRepository audienceRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public CreateAudienceUseCase(AudienceRepository audienceRepository,
                                 TourOperatorMembershipCheck membershipCheck,
                                 IdGenerator idGenerator,
                                 TransactionRunner transactionRunner,
                                 AuditTrailPort auditTrailPort) {
        this.audienceRepository = audienceRepository;
        this.membershipCheck = membershipCheck;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public UUID execute(UUID tourOperatorId, UUID callerUserId, AudienceInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        AudienceName name = new AudienceName(input.name());
        PaxPerUnit paxPerUnit = new PaxPerUnit(input.paxPerUnit());

        if (audienceRepository.existsByTourOperatorIdAndName(tourOperatorId, name.value())) {
            throw new ResourceAlreadyExistsException("An audience with this name already exists");
        }

        Audience audience = new Audience(idGenerator.newId(), tourOperatorId, name, paxPerUnit, callerUserId);
        try {
            return transactionRunner.call(() -> {
                Audience saved = audienceRepository.save(audience);
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "AUDIENCE", saved.getId(), "audience.created", null));
                return saved;
            }).getId();
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("An audience with this name already exists");
        }
    }
}
