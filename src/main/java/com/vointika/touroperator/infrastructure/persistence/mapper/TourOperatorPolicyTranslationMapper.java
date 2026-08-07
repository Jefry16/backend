package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.touroperator.domain.entity.PolicyTranslation;
import com.vointika.touroperator.domain.valueobject.PolicyBody;
import com.vointika.touroperator.domain.valueobject.PolicyTitle;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyTranslationJpaEntity;
import com.vointika.shared.valueobject.LocaleCode;

public class TourOperatorPolicyTranslationMapper {

    public static TourOperatorPolicyTranslationJpaEntity toJpa(PolicyTranslation t) {
        return new TourOperatorPolicyTranslationJpaEntity(
                t.tourOperatorId(), t.type(), t.locale().value(),
                t.title() == null ? null : t.title().value(),
                t.body() == null ? null : t.body().value());
    }

    public static PolicyTranslation toDomain(TourOperatorPolicyTranslationJpaEntity jpa) {
        return new PolicyTranslation(
                jpa.getTourOperatorId(), jpa.getType(), new LocaleCode(jpa.getLocale()),
                jpa.getTitle() == null ? null : new PolicyTitle(jpa.getTitle()),
                jpa.getBody() == null ? null : new PolicyBody(jpa.getBody()));
    }

    private TourOperatorPolicyTranslationMapper() {}
}
