package com.vointika.contact.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * Who is writing. Optional — a shopper may not give a name, and refusing the
 * message over it would lose the operator a customer to protect a column.
 *
 * <p>The object is always present; its {@code value} is what may be null. That
 * keeps "no name" a single representation rather than two (a null holder and a
 * holder of null), which is the shape the entity relies on.
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
}
