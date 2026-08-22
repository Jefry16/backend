package com.vointika.contact.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The message itself, stored verbatim.
 *
 * <p>The cap is the reason this class exists: the column is TEXT, so without one
 * a public form is an invitation to write a novel into the operator's database.
 * Nothing here strips or escapes — the admin renders it as text, never as
 * markup, and mangling what someone wrote would be worse than storing it.
 */
public record ContactContent(String value) {

    /**
     * Public because the storefront's contact form renders a {@code maxlength}
     * from it, through {@code StorefrontContactQuery}. The domain owns the
     * number; a form that retyped it would drift the day this one moved.
     */
    public static final int MAX_LENGTH = 5000;

    public ContactContent {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Message is required");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidFieldException("Message must be at most " + MAX_LENGTH + " characters");
        }
    }
}
