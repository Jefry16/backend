package com.vointika.page.infrastructure.persistence.repository;

import com.vointika.page.infrastructure.persistence.entity.PageTranslationId;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageTranslationJpaRepository
        extends JpaRepository<PageTranslationJpaEntity, PageTranslationId> {

    Optional<PageTranslationJpaEntity> findByPageIdAndLocale(UUID pageId, String locale);

    List<PageTranslationJpaEntity> findByPageId(UUID pageId);

    /** Localized handles for many pages in one query. */
    List<PageTranslationJpaEntity> findByPageIdInAndLocale(
            java.util.Collection<UUID> pageIds, String locale);

    /** Resolves a localized handle back to its page. */
    Optional<PageTranslationJpaEntity> findByTourOperatorIdAndLocaleAndSlug(
            UUID tourOperatorId, String locale, String slug);

    boolean existsByTourOperatorIdAndLocaleAndSlugAndPageIdNot(
            UUID tourOperatorId, String locale, String slug, UUID pageId);
}
