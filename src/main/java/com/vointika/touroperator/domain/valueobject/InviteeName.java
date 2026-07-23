package com.vointika.touroperator.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The invitee's display name captured on a team invitation — the inviter's label
 * for the person, used to greet them in the invite email and to show them in the
 * pending-invitations list before they have an account. Non-blank, trimmed, ≤255.
 * Distinct from the eventual member name, which comes from identity on accept.
 */
public record InviteeName(String value) {

    public InviteeName {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Name cannot be blank");
        }
        String trimmed = value.trim();
        if (trimmed.length() > 255) {
            throw new InvalidFieldException("Name must be at most 255 characters");
        }
        value = trimmed;
    }
}
