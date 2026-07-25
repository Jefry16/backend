package com.vointika.audience.application.usecase;

import com.vointika.audience.application.dto.input.AudienceInput;
import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.audience.domain.valueobject.PaxPerUnit;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

/**
 * Updates an audience's name + pax-per-unit. ADMIN+ only. Guards: caller not
 * ADMIN+ → 403; id not under this operator → 404; name clashes with another of
 * the operator's audiences → 409 (up-front and on the index race).
 */
public class UpdateAudienceUseCase {

    private final AudienceRepository audienceRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;

    public UpdateAudienceUseCase(AudienceRepository audienceRepository,
                                 TourOperatorMembershipCheck membershipCheck,
                                 TransactionRunner transactionRunner) {
        this.audienceRepository = audienceRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
    }

    public void execute(UUID tourOperatorId, UUID audienceId, UUID callerUserId, AudienceInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        Audience audience = audienceRepository.findByIdAndTourOperatorId(audienceId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Audience not found"));

        AudienceName newName = new AudienceName(input.name());
        PaxPerUnit newPaxPerUnit = new PaxPerUnit(input.paxPerUnit());

        if (!newName.value().equals(audience.getName().value())
                && audienceRepository.existsByTourOperatorIdAndNameExcluding(
                        tourOperatorId, newName.value(), audienceId)) {
            throw new ResourceAlreadyExistsException("An audience with this name already exists");
        }

        audience.rename(newName);
        audience.changePaxPerUnit(newPaxPerUnit);

        try {
            transactionRunner.run(() -> audienceRepository.save(audience));
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("An audience with this name already exists");
        }
    }
}
