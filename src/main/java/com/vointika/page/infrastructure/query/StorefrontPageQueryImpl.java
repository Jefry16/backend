package com.vointika.page.infrastructure.query;

import com.vointika.page.domain.enums.PageStatus;
import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationJpaEntity;
import com.vointika.page.infrastructure.persistence.repository.PageJpaRepository;
import com.vointika.page.infrastructure.persistence.repository.PageTranslationJpaRepository;
import com.vointika.shared.port.StorefrontPageQuery;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * page's adapter for the storefront read seam — the mirror of
 * {@code StorefrontExperienceQueryImpl}, and deliberately so: the two content
 * types differ in their fields, not in how they are published, translated or
 * addressed.
 *
 * <p>Only PUBLISHED pages leave this class, and the locale overlay is applied
 * here because the translations belong to this context. What survives is the
 * batched handle lookup navigation needs; the detail read went with the page
 * render context.
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
    public Map<UUID, String> publishedHandles(UUID tourOperatorId, Collection<UUID> ids, String locale) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }

        List<PageJpaEntity> published = pageRepository
                .findByIdInAndTourOperatorIdAndStatus(ids, tourOperatorId, PageStatus.PUBLISHED);
        if (published.isEmpty()) {
            return Map.of();
        }

        Map<UUID, String> localized = translationRepository
                .findByPageIdInAndLocale(published.stream().map(PageJpaEntity::getId).toList(), locale)
                .stream()
                .filter(translation -> translation.getHandle() != null)
                .collect(Collectors.toMap(
                        PageTranslationJpaEntity::getPageId, PageTranslationJpaEntity::getHandle));

        Map<UUID, String> handles = new HashMap<>();
        for (PageJpaEntity page : published) {
            handles.put(page.getId(), localized.getOrDefault(page.getId(), page.getHandle()));
        }
        return Map.copyOf(handles);
    }
}
