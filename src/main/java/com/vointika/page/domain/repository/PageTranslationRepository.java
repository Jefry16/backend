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
    boolean existsByHandle(UUID tourOperatorId, LocaleCode locale, String handle, UUID excludePageId);

    /**
     * Whether another page already uses this handle as a localized handle in
     * <em>any</em> locale. The storefront resolves a handle against localized
     * handles first and canonical ones second, so the two namespaces are read as
     * one and must be validated as one — a canonical handle equal to another
     * page's localized handle silently shadows it in that locale.
     * {@code excludePageId} may be null when no page exists yet (create).
     */
    boolean existsByHandleInAnyLocale(UUID tourOperatorId, String handle, UUID excludePageId);

    void delete(UUID pageId, LocaleCode locale);
}
