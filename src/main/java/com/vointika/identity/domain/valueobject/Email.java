package com.vointika.identity.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;
import java.util.Locale;

public record Email(String value) {

    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Email cannot be blank");
        }
        if (value.length() > 255) {
            throw new InvalidFieldException("Email must be at most 255 characters");
        }
        if (!value.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new InvalidFieldException("Invalid email format");
        }
        value = value.toLowerCase(Locale.ROOT);
    }
}