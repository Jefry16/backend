package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.Locale;

/**
 * The shop's public contact address — the one a shopper writes to, distinct from
 * the owner's login address on {@code identity.users}.
 *
 * <p>Shape-checked loosely, the way {@code contact}'s does it: one {@code @}, a
 * dot in the domain, no whitespace. A stricter grammar rejects addresses that
 * work, and the only real proof is delivery, which nothing here attempts.
 *
 * <p>Lower-cased with {@link Locale#ROOT} — a mailbox is case-insensitive in
 * practice, and the ROOT locale matters because a Turkish default folds
 * {@code I} to a dotless ı (PATTERNS §11). 320 matches the column: RFC 5321's
 * 64-octet local part + {@code @} + 255-octet domain.
 */
public record TourOperatorEmail(String value) {

    public static final int MAX_LENGTH = 320;

    public TourOperatorEmail {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Email cannot be blank");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Email must be at most " + MAX_LENGTH + " characters");
        }
        int at = value.indexOf('@');
        if (at <= 0 || at != value.lastIndexOf('@') || at == value.length() - 1) {
            throw new InvalidFieldException("Email is not a valid address");
        }
        if (value.chars().anyMatch(Character::isWhitespace)) {
            throw new InvalidFieldException("Email is not a valid address");
        }
        if (value.indexOf('.', at) < 0) {
            throw new InvalidFieldException("Email is not a valid address");
        }
    }
}
