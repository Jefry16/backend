package com.vointika.metafield.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.regex.Pattern;

/**
 * A metaobject definition's type identifier ({@code size-chart},
 * {@code guide-profile}) — unique per operator, immutable after create.
 * Slug-shaped so themes can later address it with Liquid dot access.
 */
public record MetaobjectType(String value) {

    private static final Pattern PATTERN = Pattern.compile("^[a-z0-9]+(?:-[a-z0-9]+)*$");

    public MetaobjectType {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Metaobject type cannot be blank");
        }
        if (value.length() > 64 || !PATTERN.matcher(value).matches()) {
            throw new InvalidFieldException(
                    "Metaobject type must be slug-shaped (lowercase letters, digits, hyphens) and at most 64 characters");
        }
    }
}
