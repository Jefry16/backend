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
    public Policy save(Policy policy) {
        return TourOperatorPolicyMapper.toDomain(
                jpaRepository.save(TourOperatorPolicyMapper.toJpa(policy)));
    }

    @Override
    public Optional<Policy> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return jpaRepository.findByIdAndTourOperatorId(id, tourOperatorId)
                .map(TourOperatorPolicyMapper::toDomain);
    }

    @Override
    public boolean existsByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type) {
        return jpaRepository.existsByTourOperatorIdAndType(tourOperatorId, type);
    }

    @Override
    public CursorPage<Policy> list(ListQuery query) {
        return listExecutor.list(TourOperatorPolicyJpaEntity.class,
                ListPoliciesUseCase.SCHEMA, query, TourOperatorPolicyMapper::toDomain);
    }

    @Override
    public boolean deleteById(UUID id) {
        if (!jpaRepository.existsById(id)) {
            return false;
        }
        jpaRepository.deleteById(id);
        return true;
    }
}
