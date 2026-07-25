package com.vointika.experience.infrastructure.persistence.mapper;

import com.vointika.experience.domain.entity.SlotAudiencePricing;
import com.vointika.experience.infrastructure.persistence.entity.SlotAudiencePricingJpaEntity;

public class SlotAudiencePricingMapper {

    public static SlotAudiencePricingJpaEntity toJpa(SlotAudiencePricing p) {
        return new SlotAudiencePricingJpaEntity(
                p.id(), p.slotId(), p.audienceId(), p.audienceName(),
                p.price(), p.capacity(), p.paxPerUnit(), p.bookedCount());
    }

    public static SlotAudiencePricing toDomain(SlotAudiencePricingJpaEntity j) {
        return new SlotAudiencePricing(
                j.getId(), j.getSlotId(), j.getAudienceId(), j.getAudienceName(),
                j.getPrice(), j.getCapacity(), j.getPaxPerUnit(), j.getBookedCount());
    }

    private SlotAudiencePricingMapper() {}
}
