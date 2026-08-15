package com.vointika.contact.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * Who is writing. <b>Required.</b>
 *
 * <p>It was optional until 2026-08-15, on the argument that refusing a message
 * over a missing name loses the operator an enquiry. Two things overruled that.
 * The inbox is read and replied to by a person, so a row with an address and no
 * name is a worse enquiry rather than a rescued one. And a nullable column here
 * is invisible to a negative filter — {@code WHERE NOT (name LIKE 'x')} is
 * UNKNOWN when the column is null, so {@code filter[name][neq]} silently
 * dropped every nameless row.
 *
 * <p>Blank is the same as absent: a form that submits {@code "   "} has not
 * given a name, and storing whitespace would put the null back under a
 * different disguise.
 */
public record ContactName(String value) {

    private static final int MAX_LENGTH = 120;

    public ContactName {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Name is required");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new InvalidFieldException("Name must be at most " + MAX_LENGTH + " characters");
        }
    }
}
