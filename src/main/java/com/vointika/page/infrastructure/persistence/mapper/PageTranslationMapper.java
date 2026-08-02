package com.vointika.page.infrastructure.persistence.mapper;

import com.vointika.page.domain.entity.PageTranslation;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.page.infrastructure.persistence.entity.PageTranslationJpaEntity;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;

public final class PageTranslationMapper {

    private PageTranslationMapper() {}

    public static PageTranslationJpaEntity toJpa(PageTranslation t) {
        return new PageTranslationJpaEntity(
                t.pageId(),
                t.locale().value(),
                t.tourOperatorId(),
                t.handle() == null ? null : t.handle().value(),
                t.title() == null ? null : t.title().value(),
                t.body() == null ? null : t.body().value(),
                t.seoTitle() == null ? null : t.seoTitle().value(),
                t.seoDescription() == null ? null : t.seoDescription().value());
    }

    public static PageTranslation toDomain(PageTranslationJpaEntity e) {
        return new PageTranslation(
                e.getPageId(),
                e.getTourOperatorId(),
                new LocaleCode(e.getLocale()),
                e.getTitle() == null ? null : new PageTitle(e.getTitle()),
                e.getBody() == null ? null : new PageBody(e.getBody()),
                e.getSeoTitle() == null ? null : new PageSeoTitle(e.getSeoTitle()),
                e.getSeoDescription() == null ? null : new PageSeoDescription(e.getSeoDescription()),
                e.getHandle() == null ? null : new Handle(e.getHandle()));
    }
}
