package com.vointika.experience.infrastructure.integration;

import com.vointika.experience.infrastructure.persistence.repository.SlotPickupLocationJpaRepository;
import com.vointika.shared.port.SlotPickupLocationSnapshotPropagator;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.UUID;

/**
 * experience's adapter for the shared {@link SlotPickupLocationSnapshotPropagator}
 * seam: syncs a pickup's rename / time change onto its slot snapshots and removes
 * them on delete. Runs in the pickup update/delete transaction.
 */
@Component
public class SlotPickupLocationSnapshotPropagatorImpl implements SlotPickupLocationSnapshotPropagator {

    private final SlotPickupLocationJpaRepository jpaRepository;

    public SlotPickupLocationSnapshotPropagatorImpl(SlotPickupLocationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void propagate(UUID pickupLocationId, String pickupLocationName, LocalTime pickupLocationTime) {
        jpaRepository.updateSnapshotByPickupLocationId(pickupLocationId, pickupLocationName, pickupLocationTime);
    }

    @Override
    public void removeForPickupLocation(UUID pickupLocationId) {
        jpaRepository.deleteByPickupLocationId(pickupLocationId);
    }
}
