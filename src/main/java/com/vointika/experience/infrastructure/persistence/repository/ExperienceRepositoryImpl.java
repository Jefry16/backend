package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.application.usecase.ListExperiencesUseCase;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.mapper.ExperienceMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class ExperienceRepositoryImpl implements ExperienceRepository {

    private final ExperienceJpaRepository jpaRepository;
    private final CriteriaListExecutor listExecutor;

    public ExperienceRepositoryImpl(ExperienceJpaRepository jpaRepository, CriteriaListExecutor listExecutor) {
        this.jpaRepository = jpaRepository;
        this.listExecutor = listExecutor;
    }

    @Override
    public Experience save(Experience experience) {
        return ExperienceMapper.toDomain(jpaRepository.save(ExperienceMapper.toJpa(experience)));
    }

    @Override
    public Optional<Experience> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId) {
        return jpaRepository.findByIdAndTourOperatorId(id, tourOperatorId).map(ExperienceMapper::toDomain);
    }

    @Override
    public CursorPage<Experience> list(ListQuery query) {
        return listExecutor.list(
                ExperienceJpaEntity.class,
                ListExperiencesUseCase.SCHEMA,
                query,
                ExperienceMapper::toDomain);
    }

    @Override
    public boolean existsByTourOperatorIdAndSlug(UUID tourOperatorId, String slug) {
        return jpaRepository.existsByTourOperatorIdAndSlug(tourOperatorId, slug);
    }
}
