package com.vointika.experience.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * Optional SEO description override for an experience. Same limit as {@code page}'s and
 * the operator's — the width is SERP truncation, not a per-context choice.
 *
 * <p>Absence is modelled outside this type: an experience holds no override
 * rather than a blank one.
 */
public record SeoDescription(String value) {

    public SeoDescription {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("SEO description cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("SEO description contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 320) {
            throw new InvalidFieldException("SEO description must be between 1 and 320 characters");
        }
    }
}
