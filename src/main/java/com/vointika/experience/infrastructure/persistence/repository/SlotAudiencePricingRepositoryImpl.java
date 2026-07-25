package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.domain.entity.SlotAudiencePricing;
import com.vointika.experience.domain.repository.SlotAudiencePricingRepository;
import com.vointika.experience.infrastructure.persistence.mapper.SlotAudiencePricingMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class SlotAudiencePricingRepositoryImpl implements SlotAudiencePricingRepository {

    private final SlotAudiencePricingJpaRepository jpaRepository;

    public SlotAudiencePricingRepositoryImpl(SlotAudiencePricingJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SlotAudiencePricing save(SlotAudiencePricing pricing) {
        return SlotAudiencePricingMapper.toDomain(
                jpaRepository.save(SlotAudiencePricingMapper.toJpa(pricing)));
    }

    @Override
    public List<SlotAudiencePricing> findBySlotId(UUID slotId) {
        return jpaRepository.findBySlotId(slotId).stream()
                .map(SlotAudiencePricingMapper::toDomain).toList();
    }

    @Override
    public List<SlotAudiencePricing> findBySlotIds(Collection<UUID> slotIds) {
        if (slotIds.isEmpty()) return List.of();
        return jpaRepository.findBySlotIdIn(slotIds).stream()
                .map(SlotAudiencePricingMapper::toDomain).toList();
    }
}
