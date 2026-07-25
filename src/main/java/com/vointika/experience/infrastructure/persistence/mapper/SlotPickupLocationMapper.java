package com.vointika.experience.infrastructure.persistence.mapper;

import com.vointika.experience.domain.entity.SlotPickupLocation;
import com.vointika.experience.infrastructure.persistence.entity.SlotPickupLocationJpaEntity;

public class SlotPickupLocationMapper {

    public static SlotPickupLocationJpaEntity toJpa(SlotPickupLocation p) {
        return new SlotPickupLocationJpaEntity(
                p.id(), p.slotId(), p.pickupLocationId(),
                p.pickupLocationName(), p.pickupLocationTime());
    }

    public static SlotPickupLocation toDomain(SlotPickupLocationJpaEntity j) {
        return new SlotPickupLocation(
                j.getId(), j.getSlotId(), j.getPickupLocationId(),
                j.getPickupLocationName(), j.getPickupLocationTime());
    }

    private SlotPickupLocationMapper() {}
}
