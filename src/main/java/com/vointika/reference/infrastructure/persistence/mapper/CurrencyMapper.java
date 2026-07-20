package com.vointika.reference.infrastructure.persistence.mapper;

import com.vointika.reference.domain.entity.Currency;
import com.vointika.reference.infrastructure.persistence.entity.CurrencyJpaEntity;

public class CurrencyMapper {

    public static Currency toDomain(CurrencyJpaEntity jpa) {
        return new Currency(
                jpa.getId(),
                jpa.getCode(),
                jpa.getName(),
                jpa.getSymbol()
        );
    }
}
