package com.vointika.metafield.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.regex.Pattern;

/**
 * Key half of a metafield identifier ({@code namespace.key}). Unique per
 * (tour operator, owner type, namespace). Handle-shaped so themes can address
 * it with Liquid dot access.
 */
public record MetafieldKey(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public MetafieldKey {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Metafield key cannot be blank");
        }
        if (value.length() > 64 || !PATTERN.matcher(value).matches()) {
            throw new InvalidFieldException(
                    "Metafield key must be handle-shaped (lowercase letters, digits, hyphens) and at most 64 characters");
        }
    }
}
