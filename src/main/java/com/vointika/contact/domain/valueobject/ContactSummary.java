package com.vointika.contact.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/** The subject line — what the inbox list shows, so it must not be empty. */
public record ContactSummary(String value) {

    private static final int MAX_LENGTH = 200;

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
