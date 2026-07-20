package com.vointika.shared.event;

/**
 * Published when a tour operator is created, to welcome its creator (the OWNER).
 * Recipient email/name and {@code locale} (the creator's UI language) are
 * resolved at publish time and ride the event — the notification consumer never
 * queries another context. {@code operatorName} is a template variable.
 */
public record TourOperatorWelcomeEmailRequestedEvent(
        String email, String name, String operatorName, String locale) {}
