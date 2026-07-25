package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.SlotPickupLocation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SlotPickupLocationRepository {

    SlotPickupLocation save(SlotPickupLocation pickup);

    List<SlotPickupLocation> findBySlotId(UUID slotId);

    /** Batched load for a page of slots (no N+1). */
    List<SlotPickupLocation> findBySlotIds(Collection<UUID> slotIds);
}
