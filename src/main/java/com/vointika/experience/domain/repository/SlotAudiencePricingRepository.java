package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.SlotAudiencePricing;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SlotAudiencePricingRepository {

    SlotAudiencePricing save(SlotAudiencePricing pricing);

    List<SlotAudiencePricing> findBySlotId(UUID slotId);

    /** Batched load for a page of slots (no N+1). */
    List<SlotAudiencePricing> findBySlotIds(Collection<UUID> slotIds);
}
