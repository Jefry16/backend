package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** A filter/facet tag — non-blank, trimmed, ≤50 chars. Not translated. */
public record Tag(String value) {

    public Tag {
        value = normalize(value);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException("A tag cannot be blank");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 50) {
            throw new InvalidFieldException("A tag must be at most 50 characters");
        }
        return trimmed;
    }
}
