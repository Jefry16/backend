package com.vointika.pickup.application.usecase;

import com.vointika.pickup.application.dto.input.PickupLocationInput;
import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.pickup.domain.valueobject.PickupLocationName;
import com.vointika.pickup.domain.valueobject.PickupLocationTime;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Updates a pickup location's name and/or time (partial — only provided fields
 * apply). ADMIN+ only. Guards: caller not ADMIN+ → 403; id not under this
 * operator → 404; name clashes case-insensitively with another pickup → 409.
 * A no-op edit writes nothing. Standalone catalog for now — slot propagation
 * deliberately unwired while the relationship model is designed.
 */
public class UpdatePickupLocationUseCase {

    private final PickupLocationRepository pickupLocationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdatePickupLocationUseCase(PickupLocationRepository pickupLocationRepository,
                                       TourOperatorMembershipCheck membershipCheck,
                                       TransactionRunner transactionRunner,
                                       AuditTrailPort auditTrailPort) {
        this.pickupLocationRepository = pickupLocationRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID pickupLocationId, UUID callerUserId,
                        PickupLocationInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        PickupLocation pickupLocation = pickupLocationRepository
                .requireByIdAndTourOperatorId(pickupLocationId, tourOperatorId);

        Map<String, Object> before = pickupLocation.auditSnapshot();
        boolean changed = false;

        if (input.name() != null) {
            PickupLocationName newName = new PickupLocationName(input.name());
            if (!newName.value().equals(pickupLocation.getName().value())) {
                if (pickupLocationRepository.existsByTourOperatorIdAndNameExcluding(
                        tourOperatorId, newName.value(), pickupLocationId)) {
                    throw new ResourceAlreadyExistsException(PickupLocationRepository.NAME_TAKEN);
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

        List<FieldChange> changes = AuditChanges.diff(before, pickupLocation.auditSnapshot());
        try {
            transactionRunner.run(() -> {
                pickupLocationRepository.save(pickupLocation);
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "PICKUP_LOCATION", pickupLocationId, "pickup_location.updated", null, changes));
            });
        } catch (UniqueConstraintViolationException e) {
            throw new ResourceAlreadyExistsException(PickupLocationRepository.NAME_TAKEN);
        }
    }
}
