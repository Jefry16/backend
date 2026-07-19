package com.vointika.shared.event;

/**
 * Someone submitted a registration for an email that already has an account.
 * The registration endpoint responds with the same generic 201 either way
 * (no enumeration, §7.6); this event routes the truthful signal to the one
 * party allowed to learn it — the mailbox owner — as an "you already have an
 * account" notice. {@code name} is the EXISTING user's name, not whatever
 * the prospective registrant typed.
 */
public record AccountAlreadyRegisteredEvent(String email, String name) {
}
