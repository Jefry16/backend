package com.vointika.experience.infrastructure.query;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceTranslationJpaRepository;
import com.vointika.shared.port.StorefrontExperienceQuery;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * experience's adapter for the storefront read seam.
 *
 * <p>Two responsibilities the renderer must not take on: <strong>publication</strong>
 * (only published rows leave this class, so a draft cannot reach a public page
 * through a forgotten check) and <strong>locale resolution</strong> (the
 * translation overlay is applied here, because the translations belong to this
 * context).
 *
 * <p>What survives is the batched handle lookup navigation needs. The list and
 * detail reads went with the experience render contexts, and with them the
 * storefront list schema and the media batch resolver this class used to hold.
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
    public Map<UUID, String> publishedHandles(UUID tourOperatorId, Collection<UUID> ids, String locale) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        List<ExperienceJpaEntity> published =
                experienceRepository.findByIdInAndTourOperatorIdAndPublishedTrue(ids, tourOperatorId);
        if (published.isEmpty()) {
            return Map.of();
        }

        Map<UUID, ExperienceTranslationJpaEntity> overlays = overlaysFor(published, locale);

        Map<UUID, String> handles = new HashMap<>();
        for (ExperienceJpaEntity experience : published) {
            ExperienceTranslationJpaEntity overlay = overlays.get(experience.getId());
            handles.put(
                    experience.getId(),
                    overlay != null && overlay.getSlug() != null
                            ? overlay.getSlug()
                            : experience.getSlug());
        }
        return Map.copyOf(handles);
    }

    private Map<UUID, ExperienceTranslationJpaEntity> overlaysFor(
            Collection<ExperienceJpaEntity> experiences, String locale) {

        List<UUID> ids = experiences.stream().map(ExperienceJpaEntity::getId).toList();
        return translationRepository.findByExperienceIdInAndLocale(ids, locale).stream()
                .collect(Collectors.toMap(
                        ExperienceTranslationJpaEntity::getExperienceId, Function.identity()));
    }
}
