package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** A highlight bullet — non-blank, trimmed, ≤200 chars. */
public record Highlight(String value) {

    public Highlight {
        value = normalize(value);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException("A highlight cannot be blank");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 200) {
            throw new InvalidFieldException("A highlight must be at most 200 characters");
        }
        return trimmed;
    }
}
