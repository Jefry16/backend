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
import com.vointika.shared.port.StorefrontExperienceQuery;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    /**
     * The listing's schema. It declares {@code published} because the executor
     * needs the field's type to build the predicate — <b>not</b> because a caller
     * may set it. Nothing parses a request against this: the port takes a cursor,
     * so no visitor input reaches a filter at all.
     */
    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .bool("published")
            .sortable("createdAt")
            .defaultSort("-createdAt")
            .build();

    private final ExperienceJpaRepository experienceRepository;
    private final ExperienceTranslationJpaRepository translationRepository;
    private final CriteriaListExecutor listExecutor;

    public StorefrontExperienceQueryImpl(ExperienceJpaRepository experienceRepository,
                                         ExperienceTranslationJpaRepository translationRepository,
                                         CriteriaListExecutor listExecutor) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
        this.listExecutor = listExecutor;
    }

    /**
     * A page of published experiences, newest first, overlaid into {@code locale}.
     *
     * <p><b>The whole query is built here.</b> The port takes a cursor, so
     * {@code published = true} is the only filter that exists and no caller input
     * reaches one — unreachable rather than refused.
     *
     * <p><b>The overlay runs after the page is fetched</b>, which is correct only
     * because nothing sorts or filters on a translated column — the public schema
     * declares no filters and one sort, on {@code createdAt}. Add a sort on
     * {@code name} and this becomes wrong in a way no test here would catch: the
     * page would be chosen and ordered by canonical values and rendered with
     * translated ones.
     */
    @Override
    public CursorPage<ExperienceCardView> listPublished(UUID tourOperatorId, String locale, String cursor) {
        ListQuery query = new ListQuery(
                tourOperatorId,
                new FilterSpec(List.of(new Filter("published", FilterOp.EQ, Boolean.TRUE))),
                SCHEMA.defaultSort(),
                cursor);

        CursorPage<ExperienceJpaEntity> page = listExecutor.list(
                ExperienceJpaEntity.class, StorefrontExperienceQueryImpl.SCHEMA, query, e -> e);
        if (page.data().isEmpty()) {
            return new CursorPage<>(List.of(), page.nextCursor());
        }

        Map<UUID, ExperienceTranslationJpaEntity> translations = translationsFor(tourOperatorId, locale);
        return new CursorPage<>(
                page.data().stream().map(experience -> card(experience, translations.get(experience.getId()))).toList(),
                page.nextCursor());
    }

    @Override
    public List<ExperienceCardView> findFeatured(UUID tourOperatorId, String locale) {
        List<ExperienceJpaEntity> featured = experienceRepository
                .findTop12ByTourOperatorIdAndPublishedTrueAndFeaturedTrueOrderByCreatedAtAscIdAsc(tourOperatorId);
        if (featured.isEmpty()) {
            return List.of();
        }

        Map<UUID, ExperienceTranslationJpaEntity> translations = translationsFor(tourOperatorId, locale);
        return featured.stream()
                .map(experience -> card(experience, translations.get(experience.getId())))
                .toList();
    }

    /**
     * Every translation for the locale in one query, keyed by experience.
     *
     * <p>One lookup per card is the N+1 this exists to prevent, and the listing
     * makes it twenty rather than twelve.
     */
    private Map<UUID, ExperienceTranslationJpaEntity> translationsFor(UUID tourOperatorId, String locale) {
        Map<UUID, ExperienceTranslationJpaEntity> translations = new HashMap<>();
        for (ExperienceTranslationJpaEntity translation
                : translationRepository.findByTourOperatorIdAndLocale(tourOperatorId, locale)) {
            translations.put(translation.getExperienceId(), translation);
        }
        return translations;
    }

    /** Nullable-wins-canonical on all three, the handle included. */
    private static ExperienceCardView card(ExperienceJpaEntity experience,
                                           ExperienceTranslationJpaEntity translation) {
        return new ExperienceCardView(
                experience.getId(),
                overlay(translation == null ? null : translation.getHandle(), experience.getHandle()),
                overlay(translation == null ? null : translation.getName(), experience.getName()),
                overlay(translation == null ? null : translation.getDescription(), experience.getDescription()),
                experience.getStartingPrice(),
                experience.getThumbnailMediaId());
    }

    /**
     * The link-resolution half. Two reads whatever the menu holds: the published
     * experiences among the ids asked for, then their translations in the
     * rendered locale. An id absent from the answer is unpublished, deleted or
     * another operator's — all three are "there is no link here", deliberately
     * indistinguishable.
     */
    @Override
    public Map<UUID, String> findPublishedHandles(UUID tourOperatorId, Set<UUID> experienceIds, String locale) {
        if (experienceIds.isEmpty()) {
            return Map.of();
        }
        List<ExperienceJpaEntity> published =
                experienceRepository.findByTourOperatorIdAndIdInAndPublishedTrue(tourOperatorId, experienceIds);
        if (published.isEmpty()) {
            return Map.of();
        }

        Map<UUID, ExperienceTranslationJpaEntity> translations = new HashMap<>();
        for (ExperienceTranslationJpaEntity translation
                : translationRepository.findByTourOperatorIdAndLocale(tourOperatorId, locale)) {
            translations.put(translation.getExperienceId(), translation);
        }

        Map<UUID, String> handles = new HashMap<>(published.size());
        for (ExperienceJpaEntity experience : published) {
            ExperienceTranslationJpaEntity translation = translations.get(experience.getId());
            handles.put(experience.getId(),
                    overlay(translation == null ? null : translation.getHandle(), experience.getHandle()));
        }
        return handles;
    }

    /** Nullable-wins-canonical, the same overlay every translated read in this project uses. */
    private static String overlay(String translated, String canonical) {
        return translated != null ? translated : canonical;
    }
}
