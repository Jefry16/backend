package com.vointika.experience.infrastructure.persistence.mapper;

import com.vointika.experience.domain.entity.CategoryTranslation;
import com.vointika.experience.domain.valueobject.CategoryName;
import com.vointika.experience.infrastructure.persistence.entity.CategoryTranslationJpaEntity;
import com.vointika.shared.valueobject.LocaleCode;

public class CategoryTranslationMapper {

    public static CategoryTranslationJpaEntity toJpa(CategoryTranslation t) {
        return new CategoryTranslationJpaEntity(
                t.categoryId(), t.locale().value(), t.tourOperatorId(),
                t.name() == null ? null : t.name().value());
    }

    public static CategoryTranslation toDomain(CategoryTranslationJpaEntity j) {
        return new CategoryTranslation(
                j.getCategoryId(), j.getTourOperatorId(), new LocaleCode(j.getLocale()),
                j.getName() == null ? null : new CategoryName(j.getName()));
    }

    private CategoryTranslationMapper() {}
}
