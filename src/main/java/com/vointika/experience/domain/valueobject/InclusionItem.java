package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** An "included" / "not included" line — non-blank, trimmed, ≤200 chars. */
public record InclusionItem(String value) {

    public InclusionItem {
        value = normalize(value);
    }

    private static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException("An inclusion item cannot be blank");
        }
        String trimmed = raw.trim();
        if (trimmed.length() > 200) {
            throw new InvalidFieldException("An inclusion item must be at most 200 characters");
        }
        return trimmed;
    }
}
