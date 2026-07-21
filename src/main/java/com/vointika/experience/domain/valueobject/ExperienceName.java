package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** An experience's display name — 1–200 chars, trimmed, no control/format chars. */
public record ExperienceName(String value) {

    public ExperienceName {
        value = normalize(value);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException("Experience name is required");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 200) {
            throw new InvalidFieldException("Experience name must be at most 200 characters");
        }
        if (trimmed.chars().anyMatch(ExperienceName::isControlOrFormat)) {
            throw new InvalidFieldException("Experience name contains invalid characters");
        }
        return trimmed;
    }

    static boolean isControlOrFormat(int c) {
        int type = Character.getType(c);
        return type == Character.CONTROL || type == Character.FORMAT;
    }
}
