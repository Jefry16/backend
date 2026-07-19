package com.vointika.notification.application.usecase;

import com.vointika.notification.application.port.EmailSenderPort;
import com.vointika.notification.application.port.TemplateCatalog;
import com.vointika.notification.application.port.TemplateRendererPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class SendNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendNotificationUseCase.class);

    private final TemplateCatalog templateCatalog;
    private final TemplateRendererPort templateRenderer;
    private final EmailSenderPort emailSender;

    public SendNotificationUseCase(
            TemplateCatalog templateCatalog,
            TemplateRendererPort templateRenderer,
            EmailSenderPort emailSender
    ) {
        this.templateCatalog = templateCatalog;
        this.templateRenderer = templateRenderer;
        this.emailSender = emailSender;
    }

    /** English-template overload — the operator/identity emails, which stay untranslated. */
    public void execute(String notificationType, String recipientEmail, Map<String, Object> templateVariables) {
        execute(notificationType, recipientEmail, templateVariables, null);
    }

    /**
     * Sends using the template closest to {@code locale}: exact match →
     * primary subtag ({@code pt-br} → {@code pt}) → {@code 'en'}. A null or
     * unknown locale is never an error — the English base template always
     * resolves for a known type.
     */
    public void execute(String notificationType, String recipientEmail,
                        Map<String, Object> templateVariables, String locale) {
        execute(notificationType, recipientEmail, templateVariables, locale, null, null);
    }

    /**
     * The operator-flavored form (the shopper emails): {@code senderDisplayName}
     * (nullable) rides into the From header's display name and {@code replyTo}
     * (nullable) into the Reply-To header — both pass straight through to
     * {@link EmailSenderPort}; both null = the platform-branded send.
     */
    public void execute(String notificationType, String recipientEmail,
                        Map<String, Object> templateVariables, String locale,
                        String senderDisplayName, String replyTo) {
        var template = candidateLocales(locale).stream()
                .map(candidate -> templateCatalog.find(notificationType, candidate))
                .flatMap(java.util.Optional::stream)
                .findFirst();

        if (template.isEmpty()) {
            log.warn("No template found for type={} locale={}", notificationType, locale);
            return;
        }

        var t = template.get();
        // The resolved-locale line doubles as the dev verification signal for
        // the email-localization pipeline (no mail catcher in dev).
        log.info("Sending {} to {} using template locale={} (requested={})",
                notificationType, recipientEmail, t.locale(), locale);
        String renderedSubject = templateRenderer.render(t.subject(), templateVariables);
        String renderedBody = templateRenderer.render(t.body(), templateVariables);

        emailSender.send(recipientEmail, renderedSubject, renderedBody,
                senderDisplayName, replyTo);
    }

    /** Ordered, distinct: [locale, primary subtag, "en"], nulls skipped. */
    private static Set<String> candidateLocales(String locale) {
        Set<String> candidates = new LinkedHashSet<>();
        if (locale != null && !locale.isBlank()) {
            candidates.add(locale);
            int dash = locale.indexOf('-');
            if (dash > 0) {
                candidates.add(locale.substring(0, dash));
            }
        }
        candidates.add("en");
        return candidates;
    }
}
