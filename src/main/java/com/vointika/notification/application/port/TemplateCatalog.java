package com.vointika.notification.application.port;

import java.util.Optional;

/**
 * The email-template catalog — since the v2 rework templates are STATIC
 * classpath files (the {@code notification.notification_templates} table is
 * gone; per-operator overrides, if ever, return as an override layer on
 * files). Lookup is locale-EXACT — the exact → primary-subtag → {@code 'en'}
 * fallback chain lives in {@code SendNotificationUseCase}, so this stays a
 * plain keyed read (the same split the DB repository had).
 */
public interface TemplateCatalog {

    /** The template for one (type, locale), or empty when that exact pair doesn't exist. */
    Optional<EmailTemplate> find(String notificationType, String locale);

    /**
     * One loaded template. {@code locale} is the template's own language (what
     * the lookup matched — the resolved-locale log line, the dev verification
     * signal, reads it); {@code subject} and {@code body} are Thymeleaf
     * sources for the string renderer.
     */
    record EmailTemplate(String locale, String subject, String body) {}
}
