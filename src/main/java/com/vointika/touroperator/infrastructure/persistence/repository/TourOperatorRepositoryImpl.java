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
    public Optional<TourOperator> findBySlug(String slug) {
        return jpaRepository.findBySlug(slug).map(TourOperatorMapper::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepository.existsBySlug(slug);
    }

    @Override
    public boolean existsByOwnerAndName(UUID createdBy, String name) {
        return jpaRepository.existsByCreatedByAndNameIgnoreCase(createdBy, name);
    }
}
