package com.vointika.shared.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * A URL-safe handle: lowercase alphanumerics separated by single dashes, ≤170
 * chars. Shared kernel — used by tour operators (global handle) and experiences
 * (canonical slug, unique per operator). Generated from a name via
 * {@link com.vointika.shared.service.SlugGenerator}; the uniqueness scope is the
 * caller's concern.
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
