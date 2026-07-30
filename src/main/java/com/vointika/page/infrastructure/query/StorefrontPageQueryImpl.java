package com.vointika.page.infrastructure.query;

import com.vointika.page.domain.enums.PageStatus;
import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationJpaEntity;
import com.vointika.page.infrastructure.persistence.repository.PageJpaRepository;
import com.vointika.page.infrastructure.persistence.repository.PageTranslationJpaRepository;
import com.vointika.shared.port.StorefrontPageQuery;
import com.vointika.shared.port.StorefrontPageView;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * page's adapter for the storefront read seam — the mirror of
 * {@code StorefrontExperienceQueryImpl}, and deliberately so: the two content
 * types differ in their fields, not in how they are published, translated or
 * addressed.
 *
 * <p>Only PUBLISHED pages leave this class, and the locale overlay is applied
 * here because the translations belong to this context.
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
    public Optional<StorefrontPageView> findPublishedByHandle(
            UUID tourOperatorId, String handle, String locale) {

        // The localized handle for this locale wins; the canonical one stays
        // addressable either way, so translating a handle doesn't rot links.
        Optional<PageJpaEntity> found = translationRepository
                .findByTourOperatorIdAndLocaleAndSlug(tourOperatorId, locale, handle)
                .flatMap(translation -> pageRepository.findById(translation.getPageId()))
                .filter(page -> tourOperatorId.equals(page.getTourOperatorId()))
                .filter(page -> page.getStatus() == PageStatus.PUBLISHED)
                .or(() -> pageRepository.findByTourOperatorIdAndHandleAndStatus(
                        tourOperatorId, handle, PageStatus.PUBLISHED));

        return found.map(page -> {
            List<PageTranslationJpaEntity> translations =
                    translationRepository.findByPageId(page.getId());

            PageTranslationJpaEntity overlay = translations.stream()
                    .filter(translation -> locale.equals(translation.getLocale()))
                    .findFirst()
                    .orElse(null);

            return toView(page, overlay, handlesFor(translations));
        });
    }

    /** Overlay wins per field; a null translated field falls back to the canonical one. */
    private StorefrontPageView toView(PageJpaEntity page,
                                      PageTranslationJpaEntity overlay,
                                      Map<String, String> handles) {
        return new StorefrontPageView(
                pick(overlay == null ? null : overlay.getSlug(), page.getHandle()),
                pick(overlay == null ? null : overlay.getTitle(), page.getTitle()),
                pick(overlay == null ? null : overlay.getBody(), page.getBody()),
                pick(overlay == null ? null : overlay.getSeoTitle(), page.getSeoTitle()),
                pick(overlay == null ? null : overlay.getSeoDescription(), page.getSeoDescription()),
                // The template suffix is a theme concern, never translated.
                page.getTemplateSuffix(),
                page.getHandle(),
                handles);
    }

    /** Locale → localized handle, for the locales that have one. See the view's javadoc. */
    private Map<String, String> handlesFor(List<PageTranslationJpaEntity> translations) {
        Map<String, String> handles = new HashMap<>();
        for (PageTranslationJpaEntity translation : translations) {
            if (translation.getSlug() != null) {
                handles.put(translation.getLocale(), translation.getSlug());
            }
        }
        return Map.copyOf(handles);
    }

    private static String pick(String translated, String canonical) {
        return translated != null ? translated : canonical;
    }
}
