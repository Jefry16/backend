package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * A category's display name — the operator's own words, and the only content a
 * category carries. Trimmed, 1–80 characters, no control or format characters.
 */
public record CategoryName(String value) {

    public CategoryName {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Category name cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Category name contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 80) {
            throw new InvalidFieldException("Category name must be between 1 and 80 characters");
        }
    }
}
