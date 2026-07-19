package com.vointika.shared.exception;

/**
 * 410 Gone — the resource existed but is no longer usable and will not be
 * again: an expired or revoked invitation link. Distinct from 404 (never
 * existed / not yours) and from 409 (a live current-state conflict that a
 * different request could still resolve). First consumer: the team-invitation
 * accept flow.
 */
public class GoneException extends DomainException {
    public GoneException(String message) {
        super(message);
    }
}
