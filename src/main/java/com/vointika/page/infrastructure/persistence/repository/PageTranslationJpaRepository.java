package com.vointika.page.infrastructure.persistence.repository;

import com.vointika.page.infrastructure.persistence.entity.PageTranslationId;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageTranslationJpaRepository
        extends JpaRepository<PageTranslationJpaEntity, PageTranslationId> {

    Optional<PageTranslationJpaEntity> findByPageIdAndLocale(UUID pageId, String locale);

    List<PageTranslationJpaEntity> findByPageId(UUID pageId);

    /** One locale, many pages, one read — the storefront resolving menu links. */
    List<PageTranslationJpaEntity> findByPageIdInAndLocale(Collection<UUID> pageIds, String locale);

    boolean existsByTourOperatorIdAndHandleAndPageIdNot(
            UUID tourOperatorId, String handle, UUID excludePageId);

    boolean existsByTourOperatorIdAndLocaleAndHandleAndPageIdNot(
            UUID tourOperatorId, String locale, String handle, UUID pageId);
}
