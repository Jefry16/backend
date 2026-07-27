package com.vointika.page.infrastructure.persistence.mapper;

import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.projection.PageListItem;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.page.infrastructure.persistence.entity.PageJpaEntity;
import com.vointika.shared.valueobject.Slug;

public final class PageMapper {

    private PageMapper() {}

    public static PageJpaEntity toJpa(Page page) {
        return new PageJpaEntity(
                page.getId(),
                page.getTourOperatorId(),
                page.getTitle().value(),
                page.getHandle().value(),
                page.getBody().value(),
                page.getSeoTitle().map(PageSeoTitle::value).orElse(null),
                page.getSeoDescription().map(PageSeoDescription::value).orElse(null),
                page.getStatus(),
                page.getTemplateSuffix(),
                page.getCreatedBy(),
                page.getCreatedAt(),
                page.getUpdatedAt());
    }

    public static Page toDomain(PageJpaEntity e) {
        return new Page(
                e.getId(),
                e.getTourOperatorId(),
                new PageTitle(e.getTitle()),
                new Slug(e.getHandle()),
                new PageBody(e.getBody()),
                e.getSeoTitle() == null ? null : new PageSeoTitle(e.getSeoTitle()),
                e.getSeoDescription() == null ? null : new PageSeoDescription(e.getSeoDescription()),
                e.getStatus(),
                e.getTemplateSuffix(),
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public static PageListItem toListItem(PageJpaEntity e) {
        return new PageListItem(
                e.getId(),
                e.getTitle(),
                e.getHandle(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
