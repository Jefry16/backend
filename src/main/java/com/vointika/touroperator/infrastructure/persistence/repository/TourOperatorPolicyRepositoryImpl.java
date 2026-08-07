package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.application.usecase.ListPoliciesUseCase;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorPolicyMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorPolicyRepositoryImpl implements TourOperatorPolicyRepository {

    private final TourOperatorPolicyJpaRepository jpaRepository;
    private final CriteriaListExecutor listExecutor;

    public TourOperatorPolicyRepositoryImpl(TourOperatorPolicyJpaRepository jpaRepository,
                                            CriteriaListExecutor listExecutor) {
        this.jpaRepository = jpaRepository;
        this.listExecutor = listExecutor;
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
    public CursorPage<Policy> list(ListQuery query) {
        return listExecutor.list(TourOperatorPolicyJpaEntity.class,
                ListPoliciesUseCase.SCHEMA, query, TourOperatorPolicyMapper::toDomain);
    }

    @Override
    public boolean deleteByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type) {
        return jpaRepository.deleteByTourOperatorIdAndType(tourOperatorId, type) > 0;
    }
}
