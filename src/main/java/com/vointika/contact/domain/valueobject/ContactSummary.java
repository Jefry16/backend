package com.vointika.contact.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** The subject line — what the inbox list shows, so it must not be empty. */
public record ContactSummary(String value) {

    /**
     * Public because the storefront's contact form renders a {@code maxlength}
     * from it, through {@code StorefrontContactQuery}. The domain owns the
     * number; a form that retyped it would drift the day this one moved.
     */
    public static final int MAX_LENGTH = 200;

    public ContactSummary {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Subject is required");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidFieldException("Subject must be at most " + MAX_LENGTH + " characters");
        }
    }
}
