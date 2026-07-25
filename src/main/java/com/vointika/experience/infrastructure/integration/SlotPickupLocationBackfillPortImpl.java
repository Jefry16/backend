package com.vointika.experience.infrastructure.integration;

import com.vointika.experience.infrastructure.persistence.repository.SlotPickupLocationJpaRepository;
import com.vointika.shared.port.SlotPickupLocationBackfillPort;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.UUID;

/**
 * experience's adapter for the shared {@link SlotPickupLocationBackfillPort}
 * seam: links a freshly created pickup to every existing slot of the operator.
 * Runs in the pickup create transaction.
 */
@Component
public class SlotPickupLocationBackfillPortImpl implements SlotPickupLocationBackfillPort {

    private final SlotPickupLocationJpaRepository jpaRepository;

    public SlotPickupLocationBackfillPortImpl(SlotPickupLocationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void backfillForTourOperator(UUID tourOperatorId, UUID pickupLocationId,
                                        String pickupLocationName, LocalTime pickupLocationTime) {
        jpaRepository.bulkInsertForTourOperator(
                tourOperatorId, pickupLocationId, pickupLocationName, pickupLocationTime);
    }
}
