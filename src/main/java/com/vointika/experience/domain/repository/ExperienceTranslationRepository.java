package com.vointika.experience.domain.repository;

import com.vointika.experience.domain.entity.ExperienceTranslation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExperienceTranslationRepository {

    /** Create-or-replace the whole (experience, locale) overlay row. */
    ExperienceTranslation upsert(ExperienceTranslation translation);

    Optional<ExperienceTranslation> findByExperienceIdAndLocale(UUID experienceId, String locale);

    /** All translated locales for an experience (one row each). */
    List<ExperienceTranslation> findAllByExperienceId(UUID experienceId);

    /** Removes the (experience, locale) row if present; returns whether one existed. */
    boolean deleteByExperienceIdAndLocale(UUID experienceId, String locale);

    /**
     * Whether another experience already uses this localized slug for the same
     * operator + locale (the {@code excludingExperienceId} lets an experience keep
     * its own slug on re-upsert).
     */
    boolean existsByOperatorLocaleSlug(UUID tourOperatorId, String locale, String slug, UUID excludingExperienceId);
}
