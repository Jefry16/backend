package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.SlotAudiencePricingJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SlotAudiencePricingJpaRepository
        extends JpaRepository<SlotAudiencePricingJpaEntity, UUID> {

    List<SlotAudiencePricingJpaEntity> findBySlotId(UUID slotId);

    List<SlotAudiencePricingJpaEntity> findBySlotIdIn(Collection<UUID> slotIds);
}
