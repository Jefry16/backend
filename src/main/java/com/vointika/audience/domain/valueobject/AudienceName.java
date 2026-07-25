package com.vointika.audience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

public record AudienceName(String value) {

    public AudienceName {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Audience name cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Audience name contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 80) {
            throw new InvalidFieldException("Audience name must be between 1 and 80 characters");
        }
    }
}
