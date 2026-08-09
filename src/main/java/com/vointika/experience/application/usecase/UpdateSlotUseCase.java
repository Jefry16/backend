package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.UpdateSlotInput;
import com.vointika.experience.application.dto.output.SlotView;
import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.domain.entity.SlotAudiencePricing;
import com.vointika.experience.domain.repository.SlotAudiencePricingRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.experience.domain.valueobject.Capacity;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Edits a slot's per-audience capacity. ADMIN+. 404 if not under this operator;
 * editing a cancelled slot → 409. A new capacity below the tier's already-booked
 * count → 422 (all-or-nothing).
 *
 * <p>Status is not editable here. SOLD_OUT is derived from bookings at checkout,
 * not an operator toggle, and CANCELLED is terminal with its own endpoint.
 */
public class UpdateSlotUseCase {

    private final SlotRepository slotRepository;
    private final SlotAudiencePricingRepository pricingRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdateSlotUseCase(SlotRepository slotRepository,
                             SlotAudiencePricingRepository pricingRepository,
                             TourOperatorMembershipCheck membershipCheck,
                             TransactionRunner transactionRunner,
                             AuditTrailPort auditTrailPort) {
        this.slotRepository = slotRepository;
        this.pricingRepository = pricingRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public SlotView execute(UUID tourOperatorId, UUID slotId, UUID callerUserId, UpdateSlotInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        return transactionRunner.call(() -> {
            Slot slot = slotRepository.findByIdAndTourOperatorId(slotId, tourOperatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
            slot.ensureEditable();

            List<FieldChange> changes = new ArrayList<>();
            List<String> changedAudiences = new ArrayList<>();

            if (input.capacities() != null && !input.capacities().isEmpty()) {
                applyCapacities(slot.id(), input.capacities(), changes, changedAudiences);
            }

            // A no-op PATCH (same capacities) records nothing.
            if (!changes.isEmpty()) {
                Map<String, Object> details = changedAudiences.isEmpty()
                        ? null
                        // One repeated "capacity" diff per changed tier; the tier
                        // names ride details, positionally aligned with the diffs.
                        : Map.of("audiences", changedAudiences);
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "SLOT", slotId, "slot.updated", details, changes));
            }

            return SlotView.from(slot, pricingRepository.findBySlotId(slot.id()));
        });
    }

    private void applyCapacities(UUID slotId, List<UpdateSlotInput.TierCapacity> tiers,
                                 List<FieldChange> changes, List<String> changedAudiences) {
        Map<UUID, SlotAudiencePricing> byAudience = new HashMap<>();
        for (SlotAudiencePricing row : pricingRepository.findBySlotId(slotId)) {
            byAudience.put(row.audienceId(), row);
        }
        for (UpdateSlotInput.TierCapacity tier : tiers) {
            SlotAudiencePricing row = byAudience.get(tier.audienceId());
            if (row == null) {
                throw new InvalidFieldException("Audience is not priced on this slot");
            }
            int newCapacity = new Capacity(tier.capacity()).value();
            if (newCapacity < row.bookedCount()) {
                throw new InvalidFieldException("Capacity cannot be below the seats already booked");
            }
            if (newCapacity != row.capacity()) {
                changes.add(new FieldChange("capacity", row.capacity(), newCapacity));
                changedAudiences.add(row.audienceName());
            }
            pricingRepository.save(row.withCapacity(newCapacity));
        }
    }
}
