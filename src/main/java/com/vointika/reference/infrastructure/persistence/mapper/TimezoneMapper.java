package com.vointika.reference.infrastructure.persistence.mapper;

import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.infrastructure.persistence.entity.TimezoneJpaEntity;

public class TimezoneMapper {

    public static Timezone toDomain(TimezoneJpaEntity jpa) {
        return new Timezone(
                jpa.getId(),
                jpa.getName(),
                jpa.getCityName(),
                CountryMapper.toDomain(jpa.getCountry())
        );
    }
}
