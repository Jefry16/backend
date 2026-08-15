package com.vointika.contact.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The sender's reply address — the one field the operator actually needs, since
 * an inbox message they cannot answer is worthless.
 *
 * <p>Shape-checked only, and deliberately loosely: this is a stranger typing on
 * a public form, not an account being created. A too-strict pattern rejects real
 * addresses, and the real proof of validity is whether the reply arrives.
 */
public record ContactEmail(String value) {

    private static final int MAX_LENGTH = 320;

    public ContactEmail {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Email is required");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidFieldException("Email must be at most " + MAX_LENGTH + " characters");
        }
        if (!value.matches("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$")) {
            throw new InvalidFieldException("Invalid email");
        }
    }
}
