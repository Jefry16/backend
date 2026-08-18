package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.touroperator.domain.entity.TourOperatorTranslation;
import com.vointika.touroperator.domain.valueobject.BrandShortDescription;
import com.vointika.touroperator.domain.valueobject.BrandSlogan;
import com.vointika.shared.valueobject.SeoDescription;
import com.vointika.shared.valueobject.SeoTitle;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorTranslationJpaEntity;
import com.vointika.shared.valueobject.LocaleCode;

public class TourOperatorTranslationMapper {

    public static TourOperatorTranslationJpaEntity toJpa(TourOperatorTranslation t) {
        return new TourOperatorTranslationJpaEntity(
                t.tourOperatorId(),
                t.locale().value(),
                t.seoTitle() == null ? null : t.seoTitle().value(),
                t.seoDescription() == null ? null : t.seoDescription().value(),
                t.passwordMessage(),
                t.slogan() == null ? null : t.slogan().value(),
                t.shortDescription() == null ? null : t.shortDescription().value());
    }

    public static TourOperatorTranslation toDomain(TourOperatorTranslationJpaEntity jpa) {
        return new TourOperatorTranslation(
                jpa.getTourOperatorId(),
                new LocaleCode(jpa.getLocale()),
                jpa.getSeoTitle() == null ? null : new SeoTitle(jpa.getSeoTitle()),
                jpa.getSeoDescription() == null ? null : new SeoDescription(jpa.getSeoDescription()),
                jpa.getPasswordMessage(),
                jpa.getSlogan() == null ? null : new BrandSlogan(jpa.getSlogan()),
                jpa.getShortDescription() == null
                        ? null : new BrandShortDescription(jpa.getShortDescription()));
    }

    private TourOperatorTranslationMapper() {}
}
