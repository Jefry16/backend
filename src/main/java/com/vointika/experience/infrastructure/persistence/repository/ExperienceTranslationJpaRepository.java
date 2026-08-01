package com.vointika.experience.infrastructure.persistence.repository;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperienceTranslationJpaRepository
        extends JpaRepository<ExperienceTranslationJpaEntity, ExperienceTranslationId> {

    Optional<ExperienceTranslationJpaEntity> findByExperienceIdAndLocale(UUID experienceId, String locale);

    List<ExperienceTranslationJpaEntity> findByExperienceId(UUID experienceId);

    /** Batched overlay fetch — one query per page render, never one per row. */
    List<ExperienceTranslationJpaEntity> findByExperienceIdInAndLocale(
            java.util.Collection<UUID> experienceIds, String locale);


    boolean existsByTourOperatorIdAndLocaleAndSlugAndExperienceIdNot(
            UUID tourOperatorId, String locale, String slug, UUID experienceId);

    boolean existsByTourOperatorIdAndSlug(UUID tourOperatorId, String slug);
}
