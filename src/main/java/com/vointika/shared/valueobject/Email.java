package com.vointika.shared.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.Locale;

/**
 * A person's email address — non-blank, ≤255 characters, {@code local@domain.tld}
 * with no whitespace, trimmed and lower-cased.
 *
 * <p><b>Shared because two contexts depend on agreeing, not merely on being
 * similar.</b> A team invitation is addressed to an {@code identity} account that
 * may not exist yet, and the already-a-member guard matches the invitee's address
 * against identity's stored one. That lookup is only reliable while both sides
 * normalise identically — so this is one rule, and it used to be written twice.
 *
 * <p>They had already drifted: {@code touroperator}'s copy trimmed and identity's
 * did not, so {@code " a@b.co "} was a valid invitee address and an invalid login.
 * <b>The trim is the survivor</b> — it is the more forgiving of the two, a pasted
 * address with a stray space is a user slip rather than a different person, and it
 * is what the cross-context match already assumed.
 *
 * <p>Lower-cased with {@link Locale#ROOT}: a Turkish default folds {@code I} to a
 * dotless ı and would silently produce a different address (PATTERNS §11).
 *
 * <p><b>Not for every address field.</b> {@code TourOperatorEmail} is the shop's
 * public contact address — 320 characters per RFC 5321, deliberately looser, its own
 * refusals — and merging it here would couple a shopfront detail to how people log
 * in. Same shape, different fact.
 */
public record Email(String value) {

    public static final int MAX_LENGTH = 255;

    private static final String SHAPE = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";

    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Email cannot be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Email must be at most " + MAX_LENGTH + " characters");
        }
        if (!value.matches(SHAPE)) {
            throw new InvalidFieldException("Invalid email format");
        }
    }
}
