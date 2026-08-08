package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.Locale;

/**
 * One colour in the brand palette, as a six-digit hex triplet.
 *
 * <p><b>Normalised to lower case</b>, because the database CHECK is
 * {@code ~ '^#[0-9a-f]{6}$'} — an operator pasting {@code #0B3D5C} from a design
 * tool would otherwise be a 23514 with no message worth reading. Case-folded
 * with {@link Locale#ROOT}: under a Turkish default locale {@code "#ABCDEF"}
 * lower-cases to a dotless ı and stops matching (PATTERNS §11).
 *
 * <p>Three-digit shorthand ({@code #abc}) is rejected rather than expanded — the
 * column is {@code VARCHAR(7)} and a value that round-trips differently from
 * what was sent is worse than a refusal.
 */
public record HexColor(String value) {

    public HexColor {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Colour cannot be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!value.matches("^#[0-9a-f]{6}$")) {
            throw new InvalidFieldException(
                    "Colour must be a six-digit hex value like #0b3d5c");
        }
    }
}
