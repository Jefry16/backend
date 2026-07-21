package com.vointika.shared.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A BCP-47 locale code (lowercased) — {@code en}, {@code es}, {@code pt-br},
 * {@code zh-hans}. Validates SHAPE only (a 2–3 letter primary subtag with
 * optional {@code -subtag} parts, ≤ 8 chars total). Whether the code is actually
 * a supported content language is a separate check against the
 * {@code reference.languages} master list, done at the use-case boundary.
 *
 * <p>Value-based equality so it can live in a {@code Set} (the operator's
 * supported-locales set).
 */
public final class LocaleCode {

    private static final Pattern SHAPE = Pattern.compile("^[a-z]{2,3}(-[a-z0-9]{2,8})*$");

    private final String value;

    public LocaleCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException("Locale code is required");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 8 || !SHAPE.matcher(normalized).matches()) {
            throw new InvalidFieldException("Invalid locale code: " + raw);
        }
        this.value = normalized;
    }

    public static LocaleCode of(String raw) {
        return new LocaleCode(raw);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LocaleCode other && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
