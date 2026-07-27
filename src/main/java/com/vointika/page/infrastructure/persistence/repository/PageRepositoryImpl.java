package com.vointika.page.infrastructure.persistence.repository;

import com.vointika.page.application.usecase.ListPagesUseCase;
import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.projection.PageListItem;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import com.vointika.page.infrastructure.persistence.mapper.PageMapper;
import com.vointika.shared.infrastructure.list.CriteriaListExecutor;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PageRepositoryImpl implements PageRepository {

    private final PageJpaRepository jpa;
    private final CriteriaListExecutor listExecutor;

    public PageRepositoryImpl(PageJpaRepository jpa, CriteriaListExecutor listExecutor) {
        this.jpa = jpa;
        this.listExecutor = listExecutor;
    }

    @Override
    public Page save(Page page) {
        return PageMapper.toDomain(jpa.save(PageMapper.toJpa(page)));
    }

    @Override
    public boolean existsByIdAndTourOperatorId(UUID pageId, UUID tourOperatorId) {
        return jpa.existsByIdAndTourOperatorId(pageId, tourOperatorId);
    }

    @Override
    public Optional<Page> findByIdAndTourOperatorId(UUID pageId, UUID tourOperatorId) {
        return jpa.findByIdAndTourOperatorId(pageId, tourOperatorId).map(PageMapper::toDomain);
    }

    @Override
    public boolean existsByTourOperatorIdAndHandle(UUID tourOperatorId, String handle) {
        return jpa.existsByTourOperatorIdAndHandle(tourOperatorId, handle);
    }

    @Override
    public CursorPage<PageListItem> list(ListQuery query) {
        return listExecutor.list(
                PageJpaEntity.class,
                ListPagesUseCase.SCHEMA,
                query,
                PageMapper::toListItem);
    }

    @Override
    public void delete(UUID pageId) {
        jpa.deleteById(pageId);
    }
}
