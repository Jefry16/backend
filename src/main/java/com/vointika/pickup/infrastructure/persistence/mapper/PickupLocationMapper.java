package com.vointika.pickup.infrastructure.persistence.mapper;

import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.valueobject.PickupLocationName;
import com.vointika.pickup.domain.valueobject.PickupLocationTime;
import com.vointika.pickup.infrastructure.persistence.entity.PickupLocationJpaEntity;

public class PickupLocationMapper {

    public static PickupLocationJpaEntity toJpa(PickupLocation p) {
        return new PickupLocationJpaEntity(
                p.getId(), p.getTourOperatorId(), p.getCreatedBy(),
                p.getName().value(), p.getTime().value(), p.getCreatedAt());
    }

    public static PickupLocation toDomain(PickupLocationJpaEntity j) {
        return new PickupLocation(
                j.getId(), j.getTourOperatorId(),
                new PickupLocationName(j.getName()),
                new PickupLocationTime(j.getTime()),
                j.getCreatedBy(), j.getCreatedAt());
    }

    private PickupLocationMapper() {}
}
