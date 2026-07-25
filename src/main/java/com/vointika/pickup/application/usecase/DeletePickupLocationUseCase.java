package com.vointika.pickup.application.usecase;

import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Deletes a pickup location. ADMIN+ only. 404 if not under this operator.
 * Standalone catalog for now — no slot fallout to cascade.
 */
public class DeletePickupLocationUseCase {

    private final PickupLocationRepository pickupLocationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public DeletePickupLocationUseCase(PickupLocationRepository pickupLocationRepository,
                                       TourOperatorMembershipCheck membershipCheck) {
        this.pickupLocationRepository = pickupLocationRepository;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID pickupLocationId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        PickupLocation pickupLocation = pickupLocationRepository
                .findByIdAndTourOperatorId(pickupLocationId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup location not found"));
        pickupLocationRepository.deleteById(pickupLocation.getId());
    }
}
