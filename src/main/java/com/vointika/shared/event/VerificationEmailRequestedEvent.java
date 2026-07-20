package com.vointika.shared.event;

/** {@code locale} is the recipient's UI language (lowercase locale code) — selects the email template. */
public record VerificationEmailRequestedEvent(String email, String name, String token, String locale) {}
