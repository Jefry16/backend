package com.vointika.reference.infrastructure.persistence.mapper;

import com.vointika.reference.domain.entity.Country;
import com.vointika.reference.infrastructure.persistence.entity.CountryJpaEntity;

public class CountryMapper {

    public static Country toDomain(CountryJpaEntity jpa) {
        return new Country(
                jpa.getId(),
                jpa.getCode(),
                jpa.getName()
        );
    }
}
