package com.vointika.pickup.infrastructure.persistence.repository;

import com.vointika.pickup.application.usecase.ListPickupLocationsUseCase;
import com.vointika.pickup.domain.entity.PickupLocation;
import com.vointika.pickup.domain.repository.PickupLocationRepository;
import com.vointika.pickup.infrastructure.persistence.entity.PickupLocationJpaEntity;
import com.vointika.pickup.infrastructure.persistence.mapper.PickupLocationMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PickupLocationRepositoryImpl implements PickupLocationRepository {

    private final PickupLocationJpaRepository jpaRepository;
    private final CriteriaListExecutor listExecutor;

    public PickupLocationRepositoryImpl(PickupLocationJpaRepository jpaRepository,
                                        CriteriaListExecutor listExecutor) {
        this.jpaRepository = jpaRepository;
        this.listExecutor = listExecutor;
    }

    @Override
    public PickupLocation save(PickupLocation pickupLocation) {
        return PickupLocationMapper.toDomain(jpaRepository.save(PickupLocationMapper.toJpa(pickupLocation)));
    }

    @Override
    public Optional<PickupLocation> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return jpaRepository.findByIdAndTourOperatorId(id, tourOperatorId).map(PickupLocationMapper::toDomain);
    }

    @Override
    public CursorPage<PickupLocation> list(ListQuery query) {
        return listExecutor.list(
                PickupLocationJpaEntity.class,
                ListPickupLocationsUseCase.SCHEMA,
                query,
                PickupLocationMapper::toDomain);
    }

    @Override
    public boolean existsByTourOperatorIdAndName(UUID tourOperatorId, String name) {
        return jpaRepository.existsByTourOperatorIdAndNameIgnoreCase(tourOperatorId, name);
    }

    @Override
    public boolean existsByTourOperatorIdAndNameExcluding(UUID tourOperatorId, String name, UUID excludingId) {
        return jpaRepository.existsByTourOperatorIdAndNameIgnoreCaseAndIdNot(tourOperatorId, name, excludingId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
