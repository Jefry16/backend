package com.vointika.page.infrastructure.query;

import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationJpaEntity;
import com.vointika.page.infrastructure.persistence.repository.PageJpaRepository;
import com.vointika.page.infrastructure.persistence.repository.PageTranslationJpaRepository;
import com.vointika.shared.port.StorefrontPageQuery;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * page's implementation of the storefront's link resolution.
 *
 * <p>Two reads whatever the menu holds: the published pages among the ids asked
 * for, then their translations in the rendered locale. An id that is not in the
 * answer is unpublished, deleted, or another operator's — all three are the same
 * "there is no link here" to the caller, and deliberately so.
 */
@Component
public class StorefrontPageQueryImpl implements StorefrontPageQuery {

    private final PageJpaRepository pageRepository;
    private final PageTranslationJpaRepository translationRepository;

    public StorefrontPageQueryImpl(PageJpaRepository pageRepository,
                                   PageTranslationJpaRepository translationRepository) {
        this.pageRepository = pageRepository;
        this.translationRepository = translationRepository;
    }

    @Override
    public Map<UUID, String> findPublishedHandles(UUID tourOperatorId, Set<UUID> pageIds, String locale) {
        if (pageIds.isEmpty()) {
            return Map.of();
        }
        List<PageJpaEntity> published =
                pageRepository.findByTourOperatorIdAndIdInAndPublishedTrue(tourOperatorId, pageIds);
        if (published.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> translated = new HashMap<>();
        for (PageTranslationJpaEntity translation : translationRepository.findByPageIdInAndLocale(
                published.stream().map(PageJpaEntity::getId).toList(), locale)) {
            if (translation.getHandle() != null) {
                translated.put(translation.getPageId(), translation.getHandle());
            }
        }

        Map<UUID, String> handles = new HashMap<>(published.size());
        for (PageJpaEntity page : published) {
            handles.put(page.getId(), translated.getOrDefault(page.getId(), page.getHandle()));
        }
        return handles;
    }

    /**
     * <b>Two ways in, and the second one has a guard.</b> A handle a locale
     * publishes is looked up on the overlay first. Falling through to the
     * canonical handle is right only when this locale does not rename the page —
     * otherwise the canonical would be a second address for a page that already
     * has one here, which is the duplicate-content rule the locale prefix
     * follows.
     */
    @Override
    public Optional<PageView> findByHandle(UUID tourOperatorId, String handle, String locale) {
        Optional<PageTranslationJpaEntity> addressed =
                translationRepository.findByTourOperatorIdAndLocaleAndHandle(tourOperatorId, locale, handle);
        if (addressed.isPresent()) {
            return pageRepository.findById(addressed.get().getPageId())
                    .filter(PageJpaEntity::isPublished)
                    .filter(page -> page.getTourOperatorId().equals(tourOperatorId))
                    .map(page -> view(page, addressed.get(), handle));
        }

        return pageRepository.findByTourOperatorIdAndHandleAndPublishedTrue(tourOperatorId, handle)
                .flatMap(page -> {
                    PageTranslationJpaEntity translation =
                            translationRepository.findByPageIdAndLocale(page.getId(), locale).orElse(null);
                    boolean renamedInThisLocale =
                            translation != null && translation.getHandle() != null
                                    && !translation.getHandle().equals(handle);
                    return renamedInThisLocale
                            ? Optional.empty()
                            : Optional.of(view(page, translation, handle));
                });
    }

    /** Nullable-wins-canonical on every column, the overlay every translated read uses. */
    private static PageView view(PageJpaEntity page, PageTranslationJpaEntity translation, String handle) {
        return new PageView(
                page.getId(),
                handle,
                overlay(translation == null ? null : translation.getTitle(), page.getTitle()),
                overlay(translation == null ? null : translation.getBody(), page.getBody()),
                overlay(translation == null ? null : translation.getSeoTitle(), page.getSeoTitle()),
                overlay(translation == null ? null : translation.getSeoDescription(), page.getSeoDescription()));
    }

    private static String overlay(String translated, String canonical) {
        return translated != null ? translated : canonical;
    }
}
