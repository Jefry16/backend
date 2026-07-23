package com.vointika.shared.event;

/**
 * Published when a team invitation is created; the notification context turns it
 * into the invite email (TEAM_INVITATION_EMAIL). Carries the RAW token (the
 * emailed accept link — only its hash is at rest), the operator's display name
 * and the invited role (template variables), and {@code locale} — the inviting
 * user's UI language, since the invitee has no account/language yet.
 */
public record TeamInvitationRequestedEvent(
        String email, String name, String operatorName, String role, String token, String locale) {}
