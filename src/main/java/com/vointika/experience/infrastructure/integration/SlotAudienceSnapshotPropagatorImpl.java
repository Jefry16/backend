package com.vointika.experience.infrastructure.integration;

import com.vointika.experience.infrastructure.persistence.repository.SlotAudiencePricingJpaRepository;
import com.vointika.shared.port.SlotAudienceSnapshotPropagator;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * experience's adapter for the shared {@link SlotAudienceSnapshotPropagator}
 * seam: syncs an audience's name / pax-per-unit onto its snapshotted
 * audience_slot rows. Runs in the audience update's transaction.
 */
@Component
public class SlotAudienceSnapshotPropagatorImpl implements SlotAudienceSnapshotPropagator {

    private final SlotAudiencePricingJpaRepository jpaRepository;

    public SlotAudienceSnapshotPropagatorImpl(SlotAudiencePricingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void propagate(UUID audienceId, String audienceName, int paxPerUnit) {
        jpaRepository.updateSnapshotByAudienceId(audienceId, audienceName, paxPerUnit);
    }
}
