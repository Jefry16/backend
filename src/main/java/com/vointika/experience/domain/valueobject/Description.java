package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** The short description — required, ≤500 chars, no control/format chars. */
public record Description(String value) {

    public Description {
        value = normalize(value);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException("Description is required");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 500) {
            throw new InvalidFieldException("Description must be at most 500 characters");
        }
        if (trimmed.chars().anyMatch(ExperienceName::isControlOrFormat)) {
            throw new InvalidFieldException("Description contains invalid characters");
        }
        return trimmed;
    }
}
