package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;

public class TourOperatorMapper {

    public static TourOperatorJpaEntity toJpa(TourOperator operator) {
        return new TourOperatorJpaEntity(
                operator.getId(),
                operator.getName().value(),
                operator.getSlug().value(),
                operator.getTimezoneId(),
                operator.getCurrencyId(),
                operator.getAddress().value(),
                operator.getCreatedBy(),
                operator.getCreatedAt(),
                operator.getUpdatedAt()
        );
    }

    public static TourOperator toDomain(TourOperatorJpaEntity jpa) {
        return new TourOperator(
                jpa.getId(),
                new TourOperatorName(jpa.getName()),
                new Slug(jpa.getSlug()),
                jpa.getTimezoneId(),
                jpa.getCurrencyId(),
                new TourOperatorAddress(jpa.getAddress()),
                jpa.getCreatedBy(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt()
        );
    }
}
