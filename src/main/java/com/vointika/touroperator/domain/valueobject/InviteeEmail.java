package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.Locale;

/**
 * The email a team invitation is addressed to. Same rules and normalization as
 * identity's {@code Email} value object (value objects don't cross context
 * boundaries, so the rules are mirrored, not imported): non-blank, ≤255,
 * shape-checked ({@code local@domain.tld}, no whitespace), lowercased — the
 * lowercasing is what makes the already-a-member lookup against identity's
 * (also-lowercased) stored emails reliable.
 */
public record InviteeEmail(String value) {

    public InviteeEmail {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Email cannot be blank");
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (normalized.length() > 255) {
            throw new InvalidFieldException("Email must be at most 255 characters");
        }
        if (!looksLikeEmail(normalized)) {
            throw new InvalidFieldException("Invalid email format");
        }
        value = normalized;
    }

    // Equivalent to identity's ^[^@\s]+@[^@\s]+\.[^@\s]+$, expressed without a
    // regex: exactly one '@' with a non-empty local part, a domain of the form
    // label.label (non-empty on both sides of a dot), and no whitespace anywhere.
    private static boolean looksLikeEmail(String v) {
        int at = v.indexOf('@');
        if (at <= 0 || at != v.lastIndexOf('@') || at == v.length() - 1) {
            return false;
        }
        String domain = v.substring(at + 1);
        int dot = domain.indexOf('.');
        if (dot <= 0 || dot == domain.length() - 1) {
            return false;
        }
        for (int i = 0; i < v.length(); i++) {
            if (Character.isWhitespace(v.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
