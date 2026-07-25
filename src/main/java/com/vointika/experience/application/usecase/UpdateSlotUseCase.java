package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.UpdateSlotInput;
import com.vointika.experience.application.dto.output.SlotView;
import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.domain.entity.SlotAudiencePricing;
import com.vointika.experience.domain.repository.SlotAudiencePricingRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.experience.domain.valueobject.Capacity;
import com.vointika.experience.domain.valueobject.SlotStatus;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Edits a slot: set status (AVAILABLE / SOLD_OUT) and/or per-audience capacity.
 * ADMIN+. 404 if not under this operator. Status → CANCELLED is rejected (use
 * cancel); editing a cancelled slot → 409. A new capacity below the tier's
 * already-booked count → 422 (all-or-nothing).
 */
public class UpdateSlotUseCase {

    private final SlotRepository slotRepository;
    private final SlotAudiencePricingRepository pricingRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;

    public UpdateSlotUseCase(SlotRepository slotRepository,
                             SlotAudiencePricingRepository pricingRepository,
                             TourOperatorMembershipCheck membershipCheck,
                             TransactionRunner transactionRunner) {
        this.slotRepository = slotRepository;
        this.pricingRepository = pricingRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
    }

    public SlotView execute(UUID tourOperatorId, UUID slotId, UUID callerUserId, UpdateSlotInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        return transactionRunner.call(() -> {
            Slot slot = slotRepository.findByIdAndTourOperatorId(slotId, tourOperatorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));

            if (input.status() != null) {
                slot = slotRepository.save(slot.changeStatus(parseStatus(input.status())));
            }

            if (input.capacities() != null && !input.capacities().isEmpty()) {
                applyCapacities(slot.id(), input.capacities());
            }

            return SlotView.from(slot, pricingRepository.findBySlotId(slot.id()));
        });
    }

    private void applyCapacities(UUID slotId, List<UpdateSlotInput.TierCapacity> tiers) {
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
            pricingRepository.save(row.withCapacity(newCapacity));
        }
    }

    private static SlotStatus parseStatus(String raw) {
        try {
            return SlotStatus.valueOf(raw);
        } catch (IllegalArgumentException e) {
            throw new InvalidFieldException("Unknown slot status: " + raw);
        }
    }
}
