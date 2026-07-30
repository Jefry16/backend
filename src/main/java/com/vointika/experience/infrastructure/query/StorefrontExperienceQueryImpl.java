package com.vointika.experience.infrastructure.query;

import com.vointika.experience.infrastructure.persistence.entity.ExperienceJpaEntity;
import com.vointika.experience.infrastructure.persistence.entity.ExperienceTranslationJpaEntity;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceJpaRepository;
import com.vointika.experience.infrastructure.persistence.repository.ExperienceTranslationJpaRepository;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.Filter;
import com.vointika.shared.list.FilterOp;
import com.vointika.shared.list.FilterSpec;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontExperienceView;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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

    /**
     * The storefront's own list schema — deliberately narrower than the admin's.
     *
     * <p>`published` is declared so the forced filter below can be applied, but
     * no filter or sort is exposed to callers: the storefront builds this query
     * itself, so the only thing a visitor influences is the page cursor.
     */
    private static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .bool("published")
            .instant("createdAt")
            .sortable("createdAt")
            .sortable("id")
            .defaultSort("-createdAt")
            .build();

    private static final SortSpec NEWEST_FIRST = new SortSpec("createdAt", SortDirection.DESC);

    private final ExperienceJpaRepository experienceRepository;
    private final CriteriaListExecutor listExecutor;
    private final ExperienceTranslationJpaRepository translationRepository;
    private final MediaUrlBatchResolver mediaUrlBatchResolver;

    public StorefrontExperienceQueryImpl(ExperienceJpaRepository experienceRepository,
                                         ExperienceTranslationJpaRepository translationRepository,
                                         CriteriaListExecutor listExecutor,
                                         MediaUrlBatchResolver mediaUrlBatchResolver) {
        this.experienceRepository = experienceRepository;
        this.listExecutor = listExecutor;
        this.translationRepository = translationRepository;
        this.mediaUrlBatchResolver = mediaUrlBatchResolver;
    }

    @Override
    public CursorPage<StorefrontExperienceView> listPublished(
            UUID tourOperatorId, String locale, String cursor) {

        // The published filter is applied here, not taken from the caller: it is
        // the one predicate a storefront request must never be able to relax.
        ListQuery query = new ListQuery(
                tourOperatorId,
                new FilterSpec(List.of(new Filter("published", FilterOp.EQ, true))),
                NEWEST_FIRST,
                cursor);

        CursorPage<ExperienceJpaEntity> page =
                listExecutor.list(ExperienceJpaEntity.class, SCHEMA, query, entity -> entity);
        if (page.data().isEmpty()) {
            return CursorPage.empty();
        }

        // Enrich the page, not the table: overlays and media resolve for the
        // rows this page actually returned (PATTERNS §4b step 3).
        Map<UUID, ExperienceTranslationJpaEntity> overlays = overlaysFor(page.data(), locale);
        Map<UUID, String> mediaUrls = mediaUrlsFor(tourOperatorId, page.data());

        return new CursorPage<>(
                page.data().stream()
                        .map(experience -> toView(
                                experience, overlays.get(experience.getId()), mediaUrls, Map.of()))
                        .toList(),
                page.nextCursor());
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

        return found.map(experience -> {
            // Every translation of this one experience, in one query: the current
            // locale's overlay comes out of it, and so does the handle map a
            // detail page needs to link its own translations.
            List<ExperienceTranslationJpaEntity> translations =
                    translationRepository.findByExperienceId(experience.getId());

            ExperienceTranslationJpaEntity overlay = translations.stream()
                    .filter(translation -> locale.equals(translation.getLocale()))
                    .findFirst()
                    .orElse(null);

            return toView(
                    experience,
                    overlay,
                    mediaUrlsFor(tourOperatorId, List.of(experience)),
                    handlesFor(translations));
        });
    }

    /**
     * Locale → localized handle, for the locales that actually have one.
     *
     * <p>Paired with the view's canonical slug, which is addressable in every
     * locale, this lets a detail page link its own translations instead of
     * guessing — and guessing produces links that 404, which is exactly what
     * shipped in S3 before this existed.
     */
    private Map<String, String> handlesFor(List<ExperienceTranslationJpaEntity> translations) {
        Map<String, String> handles = new HashMap<>();
        for (ExperienceTranslationJpaEntity translation : translations) {
            if (translation.getSlug() != null) {
                handles.put(translation.getLocale(), translation.getSlug());
            }
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
                                            Map<UUID, String> mediaUrls,
                                            Map<String, String> handles) {
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
                urlFor(experience.getThumbnailMediaId(), mediaUrls),
                urlsFor(experience.getMediaIds(), mediaUrls),
                experience.getDurationMinutes(),
                experience.isFeatured(),
                experience.getSlug(),
                handles);
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

    /**
     * One media id's URL, tolerating a null id.
     *
     * <p>Not simply {@code mediaUrls.get(id)}: when nothing needed resolving the
     * map is {@link Map#of()}, and an immutable map throws on a null key rather
     * than answering absent. An experience with no thumbnail is ordinary, so it
     * must not depend on which map implementation happens to arrive.
     */
    private static String urlFor(UUID mediaId, Map<UUID, String> mediaUrls) {
        return mediaId == null ? null : mediaUrls.get(mediaId);
    }

    /** Gallery URLs in the operator's chosen order; a since-deleted media id drops out. */
    private static List<String> urlsFor(List<UUID> mediaIds, Map<UUID, String> mediaUrls) {
        if (mediaIds == null) {
            return List.of();
        }
        return mediaIds.stream().map(mediaUrls::get).filter(Objects::nonNull).toList();
    }
}
