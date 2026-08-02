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

    boolean existsByTourOperatorIdAndLocaleAndHandleAndExperienceIdNot(
            UUID tourOperatorId, String locale, String handle, UUID experienceId);

    boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle);
}
