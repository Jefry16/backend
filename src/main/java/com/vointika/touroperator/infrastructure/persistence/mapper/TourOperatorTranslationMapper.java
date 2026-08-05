package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.touroperator.domain.entity.TourOperatorTranslation;
import com.vointika.touroperator.domain.valueobject.OperatorSeoDescription;
import com.vointika.touroperator.domain.valueobject.OperatorSeoTitle;
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
                // slogan/shortDescription: the domain has neither, and the entity
                // maps both insertable/updatable = false, so these are ignored on
                // write.
                null,
                null);
    }

    public static TourOperatorTranslation toDomain(TourOperatorTranslationJpaEntity jpa) {
        return new TourOperatorTranslation(
                jpa.getTourOperatorId(),
                new LocaleCode(jpa.getLocale()),
                jpa.getSeoTitle() == null ? null : new OperatorSeoTitle(jpa.getSeoTitle()),
                jpa.getSeoDescription() == null ? null : new OperatorSeoDescription(jpa.getSeoDescription()),
                jpa.getPasswordMessage());
    }

    private TourOperatorTranslationMapper() {}
}
