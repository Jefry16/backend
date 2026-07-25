package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.domain.entity.SlotPickupLocation;
import com.vointika.experience.domain.repository.SlotPickupLocationRepository;
import com.vointika.experience.infrastructure.persistence.mapper.SlotPickupLocationMapper;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class SlotPickupLocationRepositoryImpl implements SlotPickupLocationRepository {

    private final SlotPickupLocationJpaRepository jpaRepository;

    public SlotPickupLocationRepositoryImpl(SlotPickupLocationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public SlotPickupLocation save(SlotPickupLocation pickup) {
        return SlotPickupLocationMapper.toDomain(
                jpaRepository.save(SlotPickupLocationMapper.toJpa(pickup)));
    }

    @Override
    public List<SlotPickupLocation> findBySlotId(UUID slotId) {
        return jpaRepository.findBySlotId(slotId).stream()
                .map(SlotPickupLocationMapper::toDomain).toList();
    }

    @Override
    public List<SlotPickupLocation> findBySlotIds(Collection<UUID> slotIds) {
        if (slotIds.isEmpty()) return List.of();
        return jpaRepository.findBySlotIdIn(slotIds).stream()
                .map(SlotPickupLocationMapper::toDomain).toList();
    }
}
