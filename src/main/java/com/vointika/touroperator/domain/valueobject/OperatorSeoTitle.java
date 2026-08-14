package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * Optional operator-level SEO {@code <title>} — the default a page falls back to
 * when it has no title of its own, and the only title source the home page has
 * besides the operator's name. Mirrors {@code page}'s limit (70 chars, Shopify's
 * search-listing title); the width is SERP truncation, not a per-context choice.
 *
 * <p>Absence is modelled outside this type — an operator holds no SEO title
 * rather than a blank one.
 */
public record OperatorSeoTitle(String value) {

    public OperatorSeoTitle {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("SEO title cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("SEO title contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 70) {
            throw new InvalidFieldException("SEO title must be between 1 and 70 characters");
        }
    }
}
