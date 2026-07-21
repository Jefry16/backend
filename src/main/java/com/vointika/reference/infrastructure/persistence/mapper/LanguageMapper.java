package com.vointika.reference.infrastructure.persistence.mapper;

import com.vointika.reference.domain.entity.Language;
import com.vointika.reference.infrastructure.persistence.entity.LanguageJpaEntity;

public class LanguageMapper {

    public static Language toDomain(LanguageJpaEntity jpa) {
        return new Language(jpa.getId(), jpa.getCode(), jpa.getName());
    }

    private LanguageMapper() {}
}
