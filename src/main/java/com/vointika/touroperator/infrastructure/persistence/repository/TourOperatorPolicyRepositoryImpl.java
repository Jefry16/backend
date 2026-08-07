package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorPolicyMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorPolicyRepositoryImpl implements TourOperatorPolicyRepository {

    private final TourOperatorPolicyJpaRepository jpaRepository;

    public TourOperatorPolicyRepositoryImpl(TourOperatorPolicyJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Policy upsert(Policy policy) {
        // save() merges on the assigned composite id -> create-or-replace the row.
        return TourOperatorPolicyMapper.toDomain(
                jpaRepository.save(TourOperatorPolicyMapper.toJpa(policy)));
    }

    @Override
    public Optional<Policy> findByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type) {
        return jpaRepository.findByTourOperatorIdAndType(tourOperatorId, type)
                .map(TourOperatorPolicyMapper::toDomain);
    }

    @Override
    public List<Policy> findAllByTourOperatorId(UUID tourOperatorId) {
        // Ordered by type, the same ordering the storefront footer reads.
        return jpaRepository.findByTourOperatorIdOrderByTypeAsc(tourOperatorId).stream()
                .map(TourOperatorPolicyMapper::toDomain)
                .toList();
    }

    @Override
    public boolean deleteByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type) {
        return jpaRepository.deleteByTourOperatorIdAndType(tourOperatorId, type) > 0;
    }
}
