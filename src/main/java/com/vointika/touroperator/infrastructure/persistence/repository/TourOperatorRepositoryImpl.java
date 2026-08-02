package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorRepositoryImpl implements TourOperatorRepository {

    private final TourOperatorJpaRepository jpaRepository;

    public TourOperatorRepositoryImpl(TourOperatorJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TourOperator save(TourOperator tourOperator) {
        return TourOperatorMapper.toDomain(
                jpaRepository.save(TourOperatorMapper.toJpa(tourOperator))
        );
    }

    @Override
    public Optional<TourOperator> findById(UUID id) {
        return jpaRepository.findById(id).map(TourOperatorMapper::toDomain);
    }

    @Override
    public Optional<TourOperator> findByHandle(String handle) {
        return jpaRepository.findByHandle(handle).map(TourOperatorMapper::toDomain);
    }

    @Override
    public boolean existsByHandle(String handle) {
        return jpaRepository.existsByHandle(handle);
    }

    @Override
    public boolean existsByOwnerAndName(UUID createdBy, String name) {
        return jpaRepository.existsByCreatedByAndNameIgnoreCase(createdBy, name);
    }
}
