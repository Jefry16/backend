package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * A URL-safe operator handle. Lives in touroperator (its only consumer today);
 * promote to a shared kernel when a second context needs slugs.
 */
public record Slug(String value) {

    private static final String PATTERN = "^[a-z0-9]+(?:-[a-z0-9]+)*$";

    public Slug {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Slug cannot be blank");
        }
        if (value.length() > 170) {
            throw new InvalidFieldException("Slug must be at most 170 characters");
        }
        if (!value.matches(PATTERN)) {
            throw new InvalidFieldException("Invalid slug format");
        }
    }
}
