package com.vointika.pickup.application.usecase;

import com.vointika.pickup.application.dto.input.PickupLocationInput;
import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.pickup.domain.valueobject.PickupLocationName;
import com.vointika.pickup.domain.valueobject.PickupLocationTime;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.SlotPickupLocationSnapshotPropagator;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

/**
 * Updates a pickup location's name and/or time (partial — only provided fields
 * apply). ADMIN+ only. Guards: caller not ADMIN+ → 403; id not under this
 * operator → 404; name clashes case-insensitively with another pickup → 409.
 * A change syncs name + time onto the pickup's slot snapshots in the same
 * transaction (synced catalog); a no-op edit writes and propagates nothing.
 */
public class UpdatePickupLocationUseCase {

    private final PickupLocationRepository pickupLocationRepository;
    private final SlotPickupLocationSnapshotPropagator snapshotPropagator;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;

    public UpdatePickupLocationUseCase(PickupLocationRepository pickupLocationRepository,
                                       SlotPickupLocationSnapshotPropagator snapshotPropagator,
                                       TourOperatorMembershipCheck membershipCheck,
                                       TransactionRunner transactionRunner) {
        this.pickupLocationRepository = pickupLocationRepository;
        this.snapshotPropagator = snapshotPropagator;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
    }

    public void execute(UUID tourOperatorId, UUID pickupLocationId, UUID callerUserId,
                        PickupLocationInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        PickupLocation pickupLocation = pickupLocationRepository
                .findByIdAndTourOperatorId(pickupLocationId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Pickup location not found"));

        boolean changed = false;

        if (input.name() != null) {
            PickupLocationName newName = new PickupLocationName(input.name());
            if (!newName.value().equals(pickupLocation.getName().value())) {
                if (pickupLocationRepository.existsByTourOperatorIdAndNameExcluding(
                        tourOperatorId, newName.value(), pickupLocationId)) {
                    throw new ResourceAlreadyExistsException(
                            "A pickup location with this name already exists");
                }
                pickupLocation.rename(newName);
                changed = true;
            }
        }

        if (input.time() != null) {
            PickupLocationTime newTime = new PickupLocationTime(input.time());
            if (!newTime.value().equals(pickupLocation.getTime().value())) {
                pickupLocation.changeTime(newTime);
                changed = true;
            }
        }

        if (!changed) {
            return;
        }

        try {
            transactionRunner.run(() -> {
                pickupLocationRepository.save(pickupLocation);
                snapshotPropagator.propagate(
                        pickupLocation.getId(),
                        pickupLocation.getName().value(),
                        pickupLocation.getTime().value());
            });
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("A pickup location with this name already exists");
        }
    }
}
