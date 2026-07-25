package com.vointika.pickup.application.usecase;

import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/** Reads one pickup location. Any member; 404 if not under this operator. */
public class GetPickupLocationUseCase {

    private final PickupLocationRepository pickupLocationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetPickupLocationUseCase(PickupLocationRepository pickupLocationRepository,
                                    TourOperatorMembershipCheck membershipCheck) {
        this.pickupLocationRepository = pickupLocationRepository;
        this.membershipCheck = membershipCheck;
    }

    public PickupLocation execute(UUID tourOperatorId, UUID pickupLocationId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return pickupLocationRepository.findByIdAndTourOperatorId(pickupLocationId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup location not found"));
    }
}
