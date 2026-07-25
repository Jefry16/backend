package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.application.usecase.ListSlotsUseCase;
import com.vointika.experience.domain.entity.Slot;
import com.vointika.experience.domain.repository.SlotRepository;
import com.vointika.experience.infrastructure.persistence.entity.SlotJpaEntity;
import com.vointika.experience.infrastructure.persistence.mapper.SlotMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class SlotRepositoryImpl implements SlotRepository {

    private final SlotJpaRepository jpaRepository;
    private final CriteriaListExecutor listExecutor;

    public SlotRepositoryImpl(SlotJpaRepository jpaRepository, CriteriaListExecutor listExecutor) {
        this.jpaRepository = jpaRepository;
        this.listExecutor = listExecutor;
    }

    @Override
    public Slot save(Slot slot) {
        return SlotMapper.toDomain(jpaRepository.save(SlotMapper.toJpa(slot)));
    }

    @Override
    public Optional<Slot> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return jpaRepository.findByIdAndTourOperatorId(id, tourOperatorId).map(SlotMapper::toDomain);
    }

    @Override
    public CursorPage<Slot> list(ListQuery query) {
        return listExecutor.list(
                SlotJpaEntity.class,
                ListSlotsUseCase.SCHEMA,
                query,
                SlotMapper::toDomain);
    }
}
