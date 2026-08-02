package com.vointika.page.infrastructure.persistence.repository;

import com.vointika.page.domain.entity.PageTranslation;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationId;
import com.vointika.page.infrastructure.persistence.mapper.PageTranslationMapper;
import com.vointika.shared.valueobject.LocaleCode;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PageTranslationRepositoryImpl implements PageTranslationRepository {

    /** Stands in for "exclude nothing" — no page can have the nil UUID. */
    private static final UUID NO_PAGE = new UUID(0L, 0L);

    private final PageTranslationJpaRepository jpa;

    public PageTranslationRepositoryImpl(PageTranslationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public void upsert(PageTranslation translation) {
        // save() merges on the assigned composite id → create-or-replace the row.
        jpa.save(PageTranslationMapper.toJpa(translation));
    }

    @Override
    public Optional<PageTranslation> find(UUID pageId, LocaleCode locale) {
        return jpa.findByPageIdAndLocale(pageId, locale.value())
                .map(PageTranslationMapper::toDomain);
    }

    @Override
    public List<PageTranslation> findAllByPageId(UUID pageId) {
        return jpa.findByPageId(pageId).stream()
                .map(PageTranslationMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByHandle(UUID tourOperatorId, LocaleCode locale, String handle, UUID excludePageId) {
        return jpa.existsByTourOperatorIdAndLocaleAndHandleAndPageIdNot(
                tourOperatorId, locale.value(), handle, excludePageId);
    }

    @Override
    public boolean existsByHandleInAnyLocale(UUID tourOperatorId, String handle, UUID excludePageId) {
        // A null exclusion would make the derived `…AndPageIdNot` query match
        // nothing, so create passes a sentinel that cannot be a real page id.
        return jpa.existsByTourOperatorIdAndHandleAndPageIdNot(
                tourOperatorId, handle,
                excludePageId == null ? NO_PAGE : excludePageId);
    }

    @Override
    public void delete(UUID pageId, LocaleCode locale) {
        PageTranslationId id = new PageTranslationId(pageId, locale.value());
        if (jpa.existsById(id)) {
            jpa.deleteById(id);
        }
    }
}
