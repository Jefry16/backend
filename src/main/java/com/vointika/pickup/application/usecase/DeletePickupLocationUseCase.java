package com.vointika.pickup.application.usecase;

import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.SlotPickupLocationSnapshotPropagator;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;

import java.util.UUID;

/**
 * Deletes a pickup location. ADMIN+ only. 404 if not under this operator. The
 * pickup's slot snapshots are removed in the same transaction (synced catalog —
 * a deleted pickup stops being offered on every departure). Booked carts/orders
 * will snapshot the pickup separately and stay untouched.
 */
public class DeletePickupLocationUseCase {

    private final PickupLocationRepository pickupLocationRepository;
    private final SlotPickupLocationSnapshotPropagator snapshotPropagator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;

    public DeletePickupLocationUseCase(PickupLocationRepository pickupLocationRepository,
                                       SlotPickupLocationSnapshotPropagator snapshotPropagator,
                                       TourOperatorMembershipCheck membershipCheck,
                                       TransactionRunner transactionRunner) {
        this.pickupLocationRepository = pickupLocationRepository;
        this.snapshotPropagator = snapshotPropagator;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
    }

    public void execute(UUID tourOperatorId, UUID pickupLocationId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        PickupLocation pickupLocation = pickupLocationRepository
                .findByIdAndTourOperatorId(pickupLocationId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup location not found"));
        transactionRunner.run(() -> {
            pickupLocationRepository.deleteById(pickupLocation.getId());
            snapshotPropagator.removeForPickupLocation(pickupLocation.getId());
        });
    }
}
