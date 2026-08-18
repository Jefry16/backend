package com.vointika.page.infrastructure.persistence.mapper;

import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.projection.PageListItem;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.shared.valueobject.SeoDescription;
import com.vointika.shared.valueobject.SeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import com.vointika.shared.valueobject.Handle;

public final class PageMapper {

    private PageMapper() {}

    public static PageJpaEntity toJpa(Page page) {
        return new PageJpaEntity(
                page.getId(),
                page.getTourOperatorId(),
                page.getTitle().value(),
                page.getHandle().value(),
                page.getBody().value(),
                page.getSeoTitle().map(SeoTitle::value).orElse(null),
                page.getSeoDescription().map(SeoDescription::value).orElse(null),
                page.isPublished(),
                page.getCreatedBy(),
                page.getCreatedAt(),
                page.getUpdatedAt());
    }

    public static Page toDomain(PageJpaEntity e) {
        return new Page(
                e.getId(),
                e.getTourOperatorId(),
                new PageTitle(e.getTitle()),
                new Handle(e.getHandle()),
                new PageBody(e.getBody()),
                e.getSeoTitle() == null ? null : new SeoTitle(e.getSeoTitle()),
                e.getSeoDescription() == null ? null : new SeoDescription(e.getSeoDescription()),
                e.isPublished(),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public static PageListItem toListItem(PageJpaEntity e) {
        return new PageListItem(
                e.getId(),
                e.getTitle(),
                e.getHandle(),
                e.isPublished(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
