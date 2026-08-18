package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.valueobject.SeoDescription;
import com.vointika.shared.valueobject.SeoTitle;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import com.vointika.touroperator.domain.valueobject.TourOperatorPhone;
import com.vointika.touroperator.domain.valueobject.TourOperatorEmail;
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
                operator.getHandle().value(),
                operator.getTimezoneId(),
                operator.getCurrencyId(),
                // Null-tolerant on purpose: every operator that predates V15 has
                // no address, and this is the write path they go through to get one.
                operator.getAddress() == null ? null : operator.getAddress().address1(),
                operator.getAddress() == null ? null : operator.getAddress().address2(),
                operator.getAddress() == null ? null : operator.getAddress().city(),
                operator.getAddress() == null ? null : operator.getAddress().province(),
                operator.getAddress() == null ? null : operator.getAddress().zip(),
                operator.getAddress() == null ? null : operator.getAddress().countryId(),
                operator.getPhone() == null ? null : operator.getPhone().value(),
                operator.getEmail() == null ? null : operator.getEmail().value(),
                operator.getCreatedBy(),
                operator.getCreatedAt(),
                operator.getUpdatedAt(),
                operator.getPrimaryLocale().value(),
                operator.isPasswordEnabled(),
                operator.getStorefrontPassword(),
                operator.getPasswordMessage(),
                operator.getSeoTitle() == null ? null : operator.getSeoTitle().value(),
                operator.getSeoDescription() == null ? null : operator.getSeoDescription().value(),
                operator.getOgImageMediaId(),
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
                new Handle(jpa.getHandle()),
                jpa.getTimezoneId(),
                jpa.getCurrencyId(),
                jpa.getAddress1() == null ? null : new TourOperatorAddress(
                        jpa.getAddress1(), jpa.getAddress2(), jpa.getCity(),
                        jpa.getProvince(), jpa.getZip(), jpa.getCountryId()),
                jpa.getCreatedBy(),
                jpa.getCreatedAt(),
                jpa.getUpdatedAt(),
                LocaleCode.of(jpa.getPrimaryLocale()),
                supported,
                jpa.isPasswordEnabled(),
                jpa.getStorefrontPassword(),
                jpa.getPasswordMessage(),
                jpa.getSeoTitle() == null ? null : new SeoTitle(jpa.getSeoTitle()),
                jpa.getSeoDescription() == null ? null : new SeoDescription(jpa.getSeoDescription()),
                jpa.getOgImageMediaId(),
                jpa.getPhone() == null ? null : new TourOperatorPhone(jpa.getPhone()),
                jpa.getEmail() == null ? null : new TourOperatorEmail(jpa.getEmail())
        );
    }
}
