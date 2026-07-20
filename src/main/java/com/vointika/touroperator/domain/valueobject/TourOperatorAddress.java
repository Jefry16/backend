package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

public record TourOperatorAddress(String value) {

    public TourOperatorAddress {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Tour operator address cannot be blank");
        }
        if (value.chars().anyMatch(c -> c == 0)) {
            throw new InvalidFieldException("Tour operator address contains an invalid character");
        }
        value = value.trim();
        if (value.length() > 500) {
            throw new InvalidFieldException("Tour operator address must be at most 500 characters");
        }
    }
}
