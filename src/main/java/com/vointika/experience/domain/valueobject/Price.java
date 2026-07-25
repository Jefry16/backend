package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** A slot's per-audience price, in the operator's currency. Non-negative, ≤10^10, 2dp. */
public record Price(BigDecimal value) {

    private static final BigDecimal MAX = new BigDecimal("10000000000"); // 10^10, fits NUMERIC(12,2)

    public Price {
        if (value == null) {
            throw new InvalidFieldException("Price cannot be null");
        }
        if (value.signum() < 0) {
            throw new InvalidFieldException("Price must be non-negative");
        }
        if (value.compareTo(MAX) >= 0) {
            throw new InvalidFieldException("Price is too large");
        }
        value = value.setScale(2, RoundingMode.HALF_UP);
    }
}
