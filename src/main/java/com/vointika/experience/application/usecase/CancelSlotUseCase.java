package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.output.SlotView;
import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.domain.repository.SlotAudiencePricingRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Cancels a departure (terminal). ADMIN+. 404 if not under this operator; already
 * cancelled → 409. No booking cascade yet (no booking context). Returns the
 * refreshed slot.
 */
public class CancelSlotUseCase {

    private final SlotRepository slotRepository;
    private final SlotAudiencePricingRepository pricingRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public CancelSlotUseCase(SlotRepository slotRepository,
                             SlotAudiencePricingRepository pricingRepository,
                             TourOperatorMembershipCheck membershipCheck) {
        this.slotRepository = slotRepository;
        this.pricingRepository = pricingRepository;
        this.membershipCheck = membershipCheck;
    }

    public SlotView execute(UUID tourOperatorId, UUID slotId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Slot slot = slotRepository.findByIdAndTourOperatorId(slotId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        Slot cancelled = slotRepository.save(slot.cancel());
        return SlotView.from(cancelled, pricingRepository.findBySlotId(cancelled.id()));
    }
}
