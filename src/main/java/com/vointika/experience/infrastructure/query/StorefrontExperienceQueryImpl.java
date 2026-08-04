package com.vointika.experience.infrastructure.query;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceTranslationJpaRepository;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.StorefrontExperienceQuery;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * experience's adapter for the shared {@link StorefrontExperienceQuery} seam.
 *
 * <p><b>Three queries for the whole page, whatever the card count.</b> The rows,
 * the operator's overlays for that one locale, and one
 * {@link MediaAssetBatchQuery} call for every thumbnail together — the batch port
 * takes a {@code Set} precisely so a listing is not N round trips. A media id
 * the operator no longer owns is simply absent from the result, so the card
 * renders without a thumbnail rather than erroring.
 *
 * <p>The order comes from the derived query and is passed straight through;
 * sorting again in Java would be a second copy of the rule to keep true.
 */
@Component
public class StorefrontExperienceQueryImpl implements StorefrontExperienceQuery {

    private final ExperienceJpaRepository experienceRepository;
    private final ExperienceTranslationJpaRepository translationRepository;
    private final MediaAssetBatchQuery mediaAssetBatchQuery;

    public StorefrontExperienceQueryImpl(ExperienceJpaRepository experienceRepository,
                                         ExperienceTranslationJpaRepository translationRepository,
                                         MediaAssetBatchQuery mediaAssetBatchQuery) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
        this.mediaAssetBatchQuery = mediaAssetBatchQuery;
    }

    @Override
    public List<StorefrontExperienceCard> findPublished(UUID tourOperatorId, String locale) {
        List<ExperienceJpaEntity> experiences = experienceRepository
                .findByTourOperatorIdAndPublishedTrueOrderByFeaturedDescCreatedAtAscIdAsc(tourOperatorId);
        if (experiences.isEmpty()) {
            return List.of();
        }
        Map<UUID, ExperienceTranslationJpaEntity> overlays = overlays(tourOperatorId, locale);
        Map<UUID, MediaAsset> thumbnails = thumbnails(tourOperatorId, experiences);
        return experiences.stream()
                .map(experience -> toCard(experience, overlays.get(experience.getId()), thumbnails))
                .toList();
    }

    private Map<UUID, ExperienceTranslationJpaEntity> overlays(UUID tourOperatorId, String locale) {
        return translationRepository.findByTourOperatorIdAndLocale(tourOperatorId, locale).stream()
                .collect(Collectors.toMap(ExperienceTranslationJpaEntity::getExperienceId, Function.identity()));
    }

    private Map<UUID, MediaAsset> thumbnails(UUID tourOperatorId, List<ExperienceJpaEntity> experiences) {
        Set<UUID> ids = new HashSet<>();
        for (ExperienceJpaEntity experience : experiences) {
            if (experience.getThumbnailMediaId() != null) {
                ids.add(experience.getThumbnailMediaId());
            }
        }
        return ids.isEmpty() ? Map.of() : mediaAssetBatchQuery.findAssetsByIds(tourOperatorId, ids);
    }

    private static StorefrontExperienceCard toCard(ExperienceJpaEntity experience,
                                                   ExperienceTranslationJpaEntity overlay,
                                                   Map<UUID, MediaAsset> thumbnails) {
        return new StorefrontExperienceCard(
                overlay(overlay == null ? null : overlay.getHandle(), experience.getHandle()),
                overlay(overlay == null ? null : overlay.getName(), experience.getName()),
                overlay(overlay == null ? null : overlay.getDescription(), experience.getDescription()),
                keyOf(thumbnails, experience.getThumbnailMediaId()),
                experience.getDurationMinutes());
    }

    /**
     * Only {@code null} falls back — an operator who translated a field to blank
     * meant blank. A translation without its own handle is the normal case, and
     * the canonical handle then addresses the experience in that locale too.
     */
    private static String overlay(String translated, String canonical) {
        return translated != null ? translated : canonical;
    }

    /**
     * The card carries the key alone; the rest of the asset has no reader until
     * a card renders an {@code <img>} with dimensions.
     *
     * <p>Never {@code assets.get(null)} — an immutable {@code Map.of()} throws on
     * a null key.
     */
    private static String keyOf(Map<UUID, MediaAsset> assets, UUID mediaId) {
        MediaAsset asset = mediaId == null ? null : assets.get(mediaId);
        return asset == null ? null : asset.storageKey();
    }
}
