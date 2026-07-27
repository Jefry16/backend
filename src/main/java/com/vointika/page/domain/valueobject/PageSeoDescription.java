package com.vointika.page.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * Optional SEO meta-description override (Shopify's admin allows 320 chars;
 * engines display ~160). Absence is modelled outside this type.
 */
public record PageSeoDescription(String value) {

    public PageSeoDescription {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Page SEO description cannot be blank");
        }
        for (int i = 0, len = value.length(); i < len; i++) {
            int type = Character.getType(value.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException("Page SEO description contains an invalid character");
            }
        }
        value = value.trim();
        if (value.isEmpty() || value.length() > 320) {
            throw new InvalidFieldException(
                    "Page SEO description must be between 1 and 320 characters");
        }
    }
}
