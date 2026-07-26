package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.CreateSlotsInput;
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
import com.vointika.shared.port.AudienceView;
import com.vointika.shared.port.OperatorTimezoneQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates recurring departures: one slot per weekday in {@code days} within
 * [validFrom, validTo], all at the same start/end time-of-day and pricing. Same
 * guards as the single create, applied to the window. An end time-of-day ≤ the
 * start rolls the slot's end to the next day. A window with no matching weekday
 * yields zero slots (still succeeds).
 */
public class CreateSlotsUseCase {

    private final ExperienceRepository experienceRepository;
    private final SlotRepository slotRepository;
    private final SlotAudiencePricingRepository pricingRepository;
    private final AudiencePricingResolver pricingResolver;
    private final OperatorTimezoneQuery operatorTimezoneQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;
    private final AuditTrailPort auditTrailPort;

    public CreateSlotsUseCase(ExperienceRepository experienceRepository,
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

    public void execute(CreateSlotsInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());

        Set<Integer> days = validDays(input.days());
        if (input.startTime() == null || input.endTime() == null) {
            throw new InvalidFieldException("Start and end time are required");
        }
        if (input.validFrom() == null || input.validTo() == null) {
            throw new InvalidFieldException("Validity window is required");
        }
        if (input.validTo().isBefore(input.validFrom())) {
            throw new InvalidFieldException("Validity window end must not be before its start");
        }

        transactionRunner.run(() -> {
            Experience parent = experienceRepository
                    .findByIdAndTourOperatorId(input.experienceId(), input.tourOperatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));

            ZoneId zone = operatorTimezoneQuery.findZoneId(input.tourOperatorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
            LocalDate today = LocalDate.now(zone);
            SlotScheduling.requireWithinWindow(input.validFrom(), today);
            SlotScheduling.requireWithinWindow(input.validTo(), today);

            List<AudienceView> resolved =
                    pricingResolver.validateAndResolve(input.audiencePrices(), input.tourOperatorId());

            int created = 0;
            for (LocalDate d = input.validFrom(); !d.isAfter(input.validTo()); d = d.plusDays(1)) {
                int dow = d.getDayOfWeek().getValue() % 7; // 0–6 Sunday-first
                if (!days.contains(dow)) {
                    continue;
                }
                LocalDateTime startAt = LocalDateTime.of(d, input.startTime());
                LocalDate endDate = input.endTime().isAfter(input.startTime()) ? d : d.plusDays(1);
                LocalDateTime endAt = LocalDateTime.of(endDate, input.endTime());

                Slot slot = new Slot(
                        idGenerator.newId(), parent.getId(), input.tourOperatorId(),
                        startAt, endAt,
                        parent.getName().value(), parent.getDescription().value());
                slotRepository.save(slot);

                for (SlotAudiencePricing row :
                        pricingResolver.buildRows(slot.id(), input.audiencePrices(), resolved)) {
                    pricingRepository.save(row);
                }
                created++;
            }
            // ONE entry per batch, on the EXPERIENCE's timeline — recording a
            // row per minted slot would bury the trail in near-duplicates.
            if (created > 0) {
                Map<String, Object> details = new LinkedHashMap<>();
                details.put("count", created);
                details.put("days", days.stream().sorted().toList());
                details.put("startTime", input.startTime().toString());
                details.put("endTime", input.endTime().toString());
                details.put("validFrom", input.validFrom().toString());
                details.put("validTo", input.validTo().toString());
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "EXPERIENCE", input.experienceId(), "experience.slots_created", details));
            }
        });
    }

    private static Set<Integer> validDays(List<Integer> days) {
        if (days == null || days.isEmpty()) {
            throw new InvalidFieldException("At least one day is required");
        }
        Set<Integer> set = new HashSet<>();
        for (Integer d : days) {
            if (d == null || d < 0 || d > 6) {
                throw new InvalidFieldException("Day must be 0–6 (Sunday-first)");
            }
            if (!set.add(d)) {
                throw new InvalidFieldException("Duplicate day in days");
            }
        }
        return set;
    }
}
