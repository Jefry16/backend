package com.vointika.audience.infrastructure.persistence.repository;

import com.vointika.audience.application.usecase.ListAudiencesUseCase;
import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.infrastructure.persistence.entity.AudienceJpaEntity;
import com.vointika.audience.infrastructure.persistence.mapper.AudienceMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AudienceRepositoryImpl implements AudienceRepository {

    private final AudienceJpaRepository jpaRepository;
    private final CriteriaListExecutor listExecutor;

    public AudienceRepositoryImpl(AudienceJpaRepository jpaRepository, CriteriaListExecutor listExecutor) {
        this.jpaRepository = jpaRepository;
        this.listExecutor = listExecutor;
    }

    @Override
    public Audience save(Audience audience) {
        return AudienceMapper.toDomain(jpaRepository.save(AudienceMapper.toJpa(audience)));
    }

    @Override
    public Optional<Audience> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return jpaRepository.findByIdAndTourOperatorId(id, tourOperatorId).map(AudienceMapper::toDomain);
    }

    @Override
    public CursorPage<Audience> list(ListQuery query) {
        return listExecutor.list(
                AudienceJpaEntity.class,
                ListAudiencesUseCase.SCHEMA,
                query,
                AudienceMapper::toDomain);
    }

    @Override
    public boolean existsByTourOperatorIdAndName(UUID tourOperatorId, String name) {
        return jpaRepository.existsByTourOperatorIdAndNameIgnoreCase(tourOperatorId, name);
    }

    @Override
    public boolean existsByTourOperatorIdAndNameExcluding(UUID tourOperatorId, String name, UUID excludingId) {
        return jpaRepository.existsByTourOperatorIdAndNameIgnoreCaseAndIdNot(tourOperatorId, name, excludingId);
    }

}
