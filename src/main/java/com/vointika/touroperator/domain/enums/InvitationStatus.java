package com.vointika.touroperator.domain.enums;

/**
 * Lifecycle of a team invitation. PENDING is the only live state; the other
 * three are terminal-ish audit states (an EXPIRED invitation can still be
 * revived by resend, which renews the token + expiry on the same row).
 */
public enum InvitationStatus {
    PENDING,
    ACCEPTED,
    REVOKED,
    EXPIRED
}
