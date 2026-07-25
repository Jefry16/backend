package com.vointika.audience.infrastructure.persistence.mapper;

import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.audience.domain.valueobject.PaxPerUnit;
import com.vointika.audience.infrastructure.persistence.entity.AudienceJpaEntity;

public class AudienceMapper {

    public static AudienceJpaEntity toJpa(Audience a) {
        return new AudienceJpaEntity(
                a.getId(),
                a.getTourOperatorId(),
                a.getCreatedBy(),
                a.getName().value(),
                a.getPaxPerUnit().value(),
                a.getCreatedAt());
    }

    public static Audience toDomain(AudienceJpaEntity jpa) {
        return new Audience(
                jpa.getId(),
                jpa.getTourOperatorId(),
                new AudienceName(jpa.getName()),
                new PaxPerUnit(jpa.getPaxPerUnit()),
                jpa.getCreatedBy(),
                jpa.getCreatedAt());
    }

    private AudienceMapper() {}
}
