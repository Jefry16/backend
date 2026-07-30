package com.vointika.experience.infrastructure.query;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceTranslationJpaRepository;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontExperienceView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
 * <p>Everything is batched. A list page resolves its overlays in one query and
 * its media in one more, however many experiences the operator has — a
 * storefront list is exactly where an N+1 would hurt most.
 */
@Component
public class StorefrontExperienceQueryImpl implements StorefrontExperienceQuery {

    private final ExperienceJpaRepository experienceRepository;
    private final ExperienceTranslationJpaRepository translationRepository;
    private final MediaUrlBatchResolver mediaUrlBatchResolver;

    public StorefrontExperienceQueryImpl(ExperienceJpaRepository experienceRepository,
                                         ExperienceTranslationJpaRepository translationRepository,
                                         MediaUrlBatchResolver mediaUrlBatchResolver) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
        this.mediaUrlBatchResolver = mediaUrlBatchResolver;
    }

    @Override
    public List<StorefrontExperienceView> listPublished(UUID tourOperatorId, String locale) {
        List<ExperienceJpaEntity> experiences =
                experienceRepository.findByTourOperatorIdAndPublishedTrueOrderByCreatedAtDesc(tourOperatorId);
        if (experiences.isEmpty()) {
            return List.of();
        }

        Map<UUID, ExperienceTranslationJpaEntity> overlays = overlaysFor(experiences, locale);
        Map<UUID, String> mediaUrls = mediaUrlsFor(tourOperatorId, experiences);

        return experiences.stream()
                .map(experience -> toView(experience, overlays.get(experience.getId()), mediaUrls))
                .toList();
    }

    @Override
    public Optional<StorefrontExperienceView> findPublishedBySlug(
            UUID tourOperatorId, String slug, String locale) {

        // The localized handle for this locale wins; the canonical slug stays
        // addressable either way, so a link shared before the operator
        // translated the handle does not rot.
        Optional<ExperienceTranslationJpaEntity> localized =
                translationRepository.findByTourOperatorIdAndLocaleAndSlug(tourOperatorId, locale, slug);

        Optional<ExperienceJpaEntity> found = localized
                .flatMap(translation -> experienceRepository.findById(translation.getExperienceId()))
                .filter(ExperienceJpaEntity::isPublished)
                .filter(experience -> tourOperatorId.equals(experience.getTourOperatorId()))
                .or(() -> experienceRepository.findByTourOperatorIdAndSlugAndPublishedTrue(tourOperatorId, slug));

        return found.map(experience -> toView(
                experience,
                // Reuse the row the handle lookup already returned — it is the
                // overlay for this experience in this locale. Only a canonical-
                // handle hit still has to go and fetch one.
                localized
                        .filter(translation -> experience.getId().equals(translation.getExperienceId()))
                        .orElseGet(() -> overlaysFor(List.of(experience), locale).get(experience.getId())),
                mediaUrlsFor(tourOperatorId, List.of(experience))));
    }

    private Map<UUID, ExperienceTranslationJpaEntity> overlaysFor(
            Collection<ExperienceJpaEntity> experiences, String locale) {

        List<UUID> ids = experiences.stream().map(ExperienceJpaEntity::getId).toList();
        return translationRepository.findByExperienceIdInAndLocale(ids, locale).stream()
                .collect(Collectors.toMap(
                        ExperienceTranslationJpaEntity::getExperienceId, Function.identity()));
    }

    private Map<UUID, String> mediaUrlsFor(UUID tourOperatorId, Collection<ExperienceJpaEntity> experiences) {
        List<UUID> mediaIds = new ArrayList<>();
        for (ExperienceJpaEntity experience : experiences) {
            if (experience.getThumbnailMediaId() != null) {
                mediaIds.add(experience.getThumbnailMediaId());
            }
            if (experience.getMediaIds() != null) {
                mediaIds.addAll(experience.getMediaIds());
            }
        }
        return mediaIds.isEmpty() ? Map.of() : mediaUrlBatchResolver.resolve(tourOperatorId, mediaIds);
    }

    /** Overlay wins per field; a null translated field falls back to the canonical one. */
    private StorefrontExperienceView toView(ExperienceJpaEntity experience,
                                            ExperienceTranslationJpaEntity overlay,
                                            Map<UUID, String> mediaUrls) {
        return new StorefrontExperienceView(
                pick(slugOf(overlay), experience.getSlug()),
                pick(overlay == null ? null : overlay.getName(), experience.getName()),
                pick(overlay == null ? null : overlay.getDescription(), experience.getDescription()),
                pick(overlay == null ? null : overlay.getLongDescription(), experience.getLongDescription()),
                pickList(overlay == null ? null : overlay.getHighlights(), experience.getHighlights()),
                pickList(overlay == null ? null : overlay.getIncluded(), experience.getIncluded()),
                pickList(overlay == null ? null : overlay.getNotIncluded(), experience.getNotIncluded()),
                // Tags are filter facets, deliberately not translated.
                orEmpty(experience.getTags()),
                mediaUrls.get(experience.getThumbnailMediaId()),
                urlsFor(experience.getMediaIds(), mediaUrls),
                experience.getDurationMinutes(),
                experience.isFeatured());
    }

    private static String slugOf(ExperienceTranslationJpaEntity overlay) {
        return overlay == null ? null : overlay.getSlug();
    }

    private static String pick(String translated, String canonical) {
        return translated != null ? translated : canonical;
    }

    private static List<String> pickList(List<String> translated, List<String> canonical) {
        return orEmpty(translated != null ? translated : canonical);
    }

    private static List<String> orEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    /** Gallery URLs in the operator's chosen order; a since-deleted media id drops out. */
    private static List<String> urlsFor(List<UUID> mediaIds, Map<UUID, String> mediaUrls) {
        if (mediaIds == null) {
            return List.of();
        }
        return mediaIds.stream().map(mediaUrls::get).filter(Objects::nonNull).toList();
    }
}
