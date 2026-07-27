package com.vointika.page.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * Optional SEO {@code <title>} override (Shopify's 70-char search-listing
 * title). Absence is modelled outside this type — a page holds no SEO title
 * rather than a blank one.
 */
public record PageSeoTitle(String value) {

    public PageSeoTitle {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Page SEO title cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Page SEO title contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 70) {
            throw new InvalidFieldException("Page SEO title must be between 1 and 70 characters");
        }
    }
}
