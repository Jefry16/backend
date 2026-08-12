package com.vointika.page.infrastructure.query;

import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationJpaEntity;
import com.vointika.page.infrastructure.persistence.repository.PageJpaRepository;
import com.vointika.page.infrastructure.persistence.repository.PageTranslationJpaRepository;
import com.vointika.shared.port.StorefrontPageQuery;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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
}
