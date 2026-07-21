package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.shared.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorJpaEntity;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

public class TourOperatorMapper {

    public static TourOperatorJpaEntity toJpa(TourOperator operator) {
        Set<String> supported = operator.getSupportedLocales().stream()
                .map(LocaleCode::value)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new TourOperatorJpaEntity(
                operator.getId(),
                operator.getName().value(),
                operator.getSlug().value(),
                operator.getTimezoneId(),
                operator.getCurrencyId(),
                operator.getAddress().value(),
                operator.getLogoMediaId(),
                operator.getCreatedBy(),
                operator.getCreatedAt(),
                operator.getUpdatedAt(),
                operator.getPrimaryLocale().value(),
                supported
        );
    }

    public static TourOperator toDomain(TourOperatorJpaEntity jpa) {
        Set<LocaleCode> supported = jpa.getSupportedLocales().stream()
                .map(LocaleCode::of)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        return new TourOperator(
                jpa.getId(),
                new TourOperatorName(jpa.getName()),
                new Slug(jpa.getSlug()),
                jpa.getTimezoneId(),
                jpa.getCurrencyId(),
                new TourOperatorAddress(jpa.getAddress()),
                jpa.getCreatedBy(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt(),
                jpa.getLogoMediaId(),
                LocaleCode.of(jpa.getPrimaryLocale()),
                supported
        );
    }
}
