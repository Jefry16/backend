package com.vointika.experience.infrastructure.query;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceTranslationJpaRepository;
import com.vointika.shared.port.StorefrontExperienceQuery;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * experience's implementation of the storefront's featured read.
 *
 * <p>Two queries, never one per card: the featured rows, then every translation
 * for the chosen locale in one go, overlaid in memory. The alternative — a
 * translation lookup per experience — is the N+1 the batch rule exists to
 * prevent, and it would be twelve of them.
 *
 * <p><b>The handle is overlaid too.</b> A localized handle is the address that
 * locale's card links to, so falling back to the canonical one silently would
 * point a Spanish card at the English URL.
 */
@Component
public class StorefrontExperienceQueryImpl implements StorefrontExperienceQuery {

    private final ExperienceJpaRepository experienceRepository;
    private final ExperienceTranslationJpaRepository translationRepository;

    public StorefrontExperienceQueryImpl(ExperienceJpaRepository experienceRepository,
                                         ExperienceTranslationJpaRepository translationRepository) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
    }

    @Override
    public List<ExperienceCardView> findFeatured(UUID tourOperatorId, String locale) {
        List<ExperienceJpaEntity> featured = experienceRepository
                .findTop12ByTourOperatorIdAndPublishedTrueAndFeaturedTrueOrderByCreatedAtAscIdAsc(tourOperatorId);
        if (featured.isEmpty()) {
            return List.of();
        }

        Map<UUID, ExperienceTranslationJpaEntity> translations = new HashMap<>();
        for (ExperienceTranslationJpaEntity translation
                : translationRepository.findByTourOperatorIdAndLocale(tourOperatorId, locale)) {
            translations.put(translation.getExperienceId(), translation);
        }

        return featured.stream().map(experience -> {
            ExperienceTranslationJpaEntity translation = translations.get(experience.getId());
            return new ExperienceCardView(
                    experience.getId(),
                    overlay(translation == null ? null : translation.getHandle(), experience.getHandle()),
                    overlay(translation == null ? null : translation.getName(), experience.getName()),
                    overlay(translation == null ? null : translation.getDescription(), experience.getDescription()),
                    experience.getStartingPrice(),
                    experience.getThumbnailMediaId());
        }).toList();
    }

    /** Nullable-wins-canonical, the same overlay every translated read in this project uses. */
    private static String overlay(String translated, String canonical) {
        return translated != null ? translated : canonical;
    }
}
