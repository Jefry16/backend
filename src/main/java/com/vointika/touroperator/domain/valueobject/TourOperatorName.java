package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

public record TourOperatorName(String value) {

    public TourOperatorName {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Tour operator name cannot be blank");
        }
        if (value.chars().anyMatch(c -> c == 0)) {
            throw new InvalidFieldException("Tour operator name contains an invalid character");
        }
        value = value.trim();
        if (value.length() < 2 || value.length() > 150) {
            throw new InvalidFieldException("Tour operator name must be between 2 and 150 characters");
        }
    }
}
