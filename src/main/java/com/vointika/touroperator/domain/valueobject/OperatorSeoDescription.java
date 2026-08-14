package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * Optional operator-level SEO meta-description — the last fallback for every page
 * type whose own description overrides are empty. Mirrors {@code page}'s limit
 * (320 chars, what Shopify's admin allows; engines display ~160).
 *
 * <p>Absence is modelled outside this type.
 */
public record OperatorSeoDescription(String value) {

    public OperatorSeoDescription {
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
