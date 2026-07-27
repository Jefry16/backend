package com.vointika.page.domain.repository;

import com.vointika.page.domain.entity.PageTranslation;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageTranslationRepository {

    /** Create-or-replace the whole (page, locale) overlay row. */
    void upsert(PageTranslation translation);

    Optional<PageTranslation> find(UUID pageId, LocaleCode locale);

    /** All translated locales for a page (one row each). */
    List<PageTranslation> findAllByPageId(UUID pageId);

    /** Localized-handle uniqueness per (operator, locale), excluding this page. */
    boolean existsBySlug(UUID tourOperatorId, LocaleCode locale, String slug, UUID excludePageId);

    void delete(UUID pageId, LocaleCode locale);
}
