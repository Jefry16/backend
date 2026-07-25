package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.output.SlotView;
import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.domain.repository.SlotAudiencePricingRepository;
import com.vointika.experience.domain.repository.SlotPickupLocationRepository;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/** Reads one slot with its pricing. Any member; 404 if not under this operator. */
public class GetSlotUseCase {

    private final SlotRepository slotRepository;
    private final SlotAudiencePricingRepository pricingRepository;
    private final SlotPickupLocationRepository pickupRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetSlotUseCase(SlotRepository slotRepository,
                          SlotAudiencePricingRepository pricingRepository,
                          SlotPickupLocationRepository pickupRepository,
                          TourOperatorMembershipCheck membershipCheck) {
        this.slotRepository = slotRepository;
        this.pricingRepository = pricingRepository;
        this.pickupRepository = pickupRepository;
        this.membershipCheck = membershipCheck;
    }

    public SlotView execute(UUID tourOperatorId, UUID slotId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        Slot slot = slotRepository.findByIdAndTourOperatorId(slotId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Slot not found"));
        return SlotView.from(slot, pricingRepository.findBySlotId(slot.id()),
                pickupRepository.findBySlotId(slot.id()));
    }
}
