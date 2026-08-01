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

    /**
     * Whether any experience of this operator uses this slug as a localized slug
     * in <em>any</em> locale. The storefront resolves a slug against localized
     * slugs first and canonical ones second, so the two namespaces are read as one
     * and must be validated as one (PATTERNS §4d) — a canonical slug equal to
     * another experience's localized slug silently shadows it in that locale.
     *
     * <p>No exclusion parameter: the only caller is experience creation, which has
     * no id to exclude yet.
     */
    boolean existsBySlugInAnyLocale(UUID tourOperatorId, String slug);
}
