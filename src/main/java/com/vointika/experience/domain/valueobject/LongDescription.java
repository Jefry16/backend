package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** The long description — required, ≤10,000 chars. Control chars other than newlines/tabs rejected. */
public record LongDescription(String value) {

    public LongDescription {
        value = normalize(value);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException("Long description is required");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 10_000) {
            throw new InvalidFieldException("Long description must be at most 10000 characters");
        }
        if (trimmed.chars().anyMatch(c -> {
            int type = Character.getType(c);
            boolean allowedWhitespace = c == '\n' || c == '\r' || c == '\t';
            return !allowedWhitespace && (type == Character.CONTROL || type == Character.FORMAT);
        })) {
            throw new InvalidFieldException("Long description contains invalid characters");
        }
        return trimmed;
    }
}
