package com.vointika.contact.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * Who is writing. Optional — a shopper may not give a name, and refusing the
 * message over it would lose the operator a customer to protect a column.
 */
public record ContactName(String value) {

    private static final int MAX_LENGTH = 120;

    public ContactName {
        if (value != null) {
            value = value.trim();
            if (value.isEmpty()) {
                value = null;
            } else if (value.length() > MAX_LENGTH) {
                throw new InvalidFieldException("Name must be at most " + MAX_LENGTH + " characters");
            }
        }
    }

    /** Null-safe: a blank or absent name is simply no name. */
    public static ContactName of(String raw) {
        return new ContactName(raw);
    }
}
