package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.domain.entity.ExperienceTranslation;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationId;
import com.vointika.experience.infrastructure.persistence.mapper.ExperienceTranslationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ExperienceTranslationRepositoryImpl implements ExperienceTranslationRepository {

    private final ExperienceTranslationJpaRepository jpaRepository;

    public ExperienceTranslationRepositoryImpl(ExperienceTranslationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public ExperienceTranslation upsert(ExperienceTranslation translation) {
        // save() merges on the assigned composite id → create-or-replace the row.
        return ExperienceTranslationMapper.toDomain(
                jpaRepository.save(ExperienceTranslationMapper.toJpa(translation)));
    }

    @Override
    public Optional<ExperienceTranslation> findByExperienceIdAndLocale(UUID experienceId, String locale) {
        return jpaRepository.findByExperienceIdAndLocale(experienceId, locale)
                .map(ExperienceTranslationMapper::toDomain);
    }

    @Override
    public List<ExperienceTranslation> findAllByExperienceId(UUID experienceId) {
        return jpaRepository.findByExperienceId(experienceId).stream()
                .map(ExperienceTranslationMapper::toDomain)
                .toList();
    }

    @Override
    public boolean deleteByExperienceIdAndLocale(UUID experienceId, String locale) {
        ExperienceTranslationId id = new ExperienceTranslationId(experienceId, locale);
        if (!jpaRepository.existsById(id)) {
            return false;
        }
        jpaRepository.deleteById(id);
        return true;
    }

    @Override
    public boolean existsByOperatorLocaleSlug(UUID tourOperatorId, String locale, String slug, UUID excludingExperienceId) {
        return jpaRepository.existsByTourOperatorIdAndLocaleAndSlugAndExperienceIdNot(
                tourOperatorId, locale, slug, excludingExperienceId);
    }
}
