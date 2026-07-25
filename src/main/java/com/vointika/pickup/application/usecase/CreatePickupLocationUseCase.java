package com.vointika.pickup.application.usecase;

import com.vointika.pickup.application.dto.input.PickupLocationInput;
import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.pickup.domain.valueobject.PickupLocationName;
import com.vointika.pickup.domain.valueobject.PickupLocationTime;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

/**
 * Creates a pickup location. ADMIN+ only; membership enforced by the route
 * interceptor. Names are unique per operator case-insensitively — a duplicate is
 * 409, up-front and on the unique-index race. Standalone catalog for now — the
 * pickup↔slot relationship model is under design and deliberately unwired.
 */
public class CreatePickupLocationUseCase {

    private final PickupLocationRepository pickupLocationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;

    public CreatePickupLocationUseCase(PickupLocationRepository pickupLocationRepository,
                                       TourOperatorMembershipCheck membershipCheck,
                                       TransactionRunner transactionRunner,
                                       IdGenerator idGenerator) {
        this.pickupLocationRepository = pickupLocationRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
    }

    public UUID execute(UUID tourOperatorId, UUID callerUserId, PickupLocationInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        PickupLocationName name = new PickupLocationName(input.name());
        PickupLocationTime time = new PickupLocationTime(input.time());

        if (pickupLocationRepository.existsByTourOperatorIdAndName(tourOperatorId, name.value())) {
            throw new ResourceAlreadyExistsException("A pickup location with this name already exists");
        }

        PickupLocation pickupLocation = new PickupLocation(
                idGenerator.newId(), tourOperatorId, name, time, callerUserId);
        try {
            transactionRunner.run(() -> pickupLocationRepository.save(pickupLocation));
        } catch (DataIntegrityViolationException e) {
            throw new ResourceAlreadyExistsException("A pickup location with this name already exists");
        }
        return pickupLocation.getId();
    }
}
