package com.vointika.audience.infrastructure.persistence.mapper;

import com.vointika.audience.domain.entity.AudienceTranslation;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.audience.infrastructure.persistence.entity.AudienceTranslationJpaEntity;
import com.vointika.shared.valueobject.LocaleCode;

public class AudienceTranslationMapper {

    public static AudienceTranslationJpaEntity toJpa(AudienceTranslation t) {
        return new AudienceTranslationJpaEntity(
                t.audienceId(), t.locale().value(), t.tourOperatorId(),
                t.name() == null ? null : t.name().value());
    }

    public static AudienceTranslation toDomain(AudienceTranslationJpaEntity j) {
        return new AudienceTranslation(
                j.getAudienceId(), j.getTourOperatorId(), new LocaleCode(j.getLocale()),
                j.getName() == null ? null : new AudienceName(j.getName()));
    }

    private AudienceTranslationMapper() {}
}
