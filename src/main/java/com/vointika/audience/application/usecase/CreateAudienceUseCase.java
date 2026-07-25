package com.vointika.audience.application.usecase;

import com.vointika.audience.application.dto.input.AudienceInput;
import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.audience.domain.valueobject.PaxPerUnit;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
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

    public CreateAudienceUseCase(AudienceRepository audienceRepository,
                                 TourOperatorMembershipCheck membershipCheck,
                                 IdGenerator idGenerator,
                                 TransactionRunner transactionRunner) {
        this.audienceRepository = audienceRepository;
        this.membershipCheck = membershipCheck;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
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
            return transactionRunner.call(() -> audienceRepository.save(audience)).getId();
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("An audience with this name already exists");
        }
    }
}
