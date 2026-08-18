package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.CreateSlotInput;
import com.vointika.experience.application.service.AudiencePricingResolver;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.domain.entity.SlotAudiencePricing;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.SlotAudiencePricingRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.OperatorTimezoneQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Creates one departure. ADMIN+. Guards: caller not ADMIN+ → 403; experience not
 * under this operator → 404; start in the past or > 24 months ahead → 422; end
 * not after start / span > 24h → 422 (Slot invariants); unknown audience → 404.
 * Snapshots the experience name/description; freezes per-audience price/capacity.
 */
public class CreateSlotUseCase {

    private final ExperienceRepository experienceRepository;
    private final SlotRepository slotRepository;
    private final SlotAudiencePricingRepository pricingRepository;
    private final AudiencePricingResolver pricingResolver;
    private final OperatorTimezoneQuery operatorTimezoneQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;
    private final AuditTrailPort auditTrailPort;

    public CreateSlotUseCase(ExperienceRepository experienceRepository,
                             SlotRepository slotRepository,
                             SlotAudiencePricingRepository pricingRepository,
                             AudiencePricingResolver pricingResolver,
                             OperatorTimezoneQuery operatorTimezoneQuery,
                             TourOperatorMembershipCheck membershipCheck,
                             TransactionRunner transactionRunner,
                             IdGenerator idGenerator,
                             AuditTrailPort auditTrailPort) {
        this.experienceRepository = experienceRepository;
        this.slotRepository = slotRepository;
        this.pricingRepository = pricingRepository;
        this.pricingResolver = pricingResolver;
        this.operatorTimezoneQuery = operatorTimezoneQuery;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
        this.auditTrailPort = auditTrailPort;
    }

    public UUID execute(CreateSlotInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());

        return transactionRunner.call(() -> {
            Experience parent = experienceRepository
                    .requireByIdAndTourOperatorId(input.experienceId(), input.tourOperatorId());

            if (input.startAt() == null) {
                throw new InvalidFieldException("Start is required");
            }
            // Empty means the operator is gone, not the experience — the seam
            // resolves the operator's own timezone, and the column is NOT NULL.
            ZoneId zone = operatorTimezoneQuery.findZoneId(input.tourOperatorId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            TourOperatorMembershipCheck.TENANT_NOT_FOUND));
            SlotScheduling.requireWithinWindow(input.startAt().toLocalDate(), LocalDate.now(zone));

            List<AudiencePricingResolver.PricedAudience> resolved =
                    pricingResolver.validateAndResolve(input.audiencePrices(), input.tourOperatorId());

            Slot slot = new Slot(
                    idGenerator.newId(), parent.getId(), input.tourOperatorId(),
                    input.startAt(), input.endAt(),
                    parent.getName().value(), parent.getDescription().value());
            slotRepository.save(slot);

            for (SlotAudiencePricing row : pricingResolver.buildRows(slot.id(), resolved)) {
                pricingRepository.save(row);
            }
            auditTrailPort.append(new NewAuditEntry(
                    input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                    "SLOT", slot.id(), "slot.created",
                    Map.of("experienceId", input.experienceId().toString())));
            return slot.id();
        });
    }
}
