package com.vointika.metafield.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.regex.Pattern;

/**
 * Namespace half of a metafield identifier ({@code namespace.key}). Groups an
 * operator's related definitions (e.g. {@code custom.difficulty}).
 * Handle-shaped so themes can address it with Liquid dot access.
 */
public record MetafieldNamespace(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public MetafieldNamespace {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Metafield namespace cannot be blank");
        }
        if (value.length() > 64 || !PATTERN.matcher(value).matches()) {
            throw new InvalidFieldException(
                    "Metafield namespace must be handle-shaped (lowercase letters, digits, hyphens) and at most 64 characters");
        }
    }
}
