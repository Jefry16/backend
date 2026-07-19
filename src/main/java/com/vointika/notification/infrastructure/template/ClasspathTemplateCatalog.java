package com.vointika.notification.infrastructure.template;

import com.vointika.notification.application.port.TemplateCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads every email template from the classpath at startup into an in-memory map
 * (templates are static — there is no DB table).
 *
 * <p><b>File naming scheme</b> — two files per (type, locale) under
 * {@code classpath:templates/email/}:
 * <pre>
 *   {type-kebab}_{locale}.html          — the Thymeleaf HTML body
 *   {type-kebab}_{locale}.subject.txt   — the (Thymeleaf) subject line
 * </pre>
 * where {@code type-kebab} is the notification type lowercased with underscores
 * turned into hyphens ({@code VERIFICATION_EMAIL} → {@code verification-email})
 * and {@code locale} is the template's own language tag. The known
 * (type → locales) universe is declared in {@link #KNOWN_TEMPLATES}; growing a
 * language (or a type) = add the file pair + the entry here.
 *
 * <p><b>Fail fast at boot</b>: a missing or blank file for any declared
 * (type, locale) throws at bean construction, so a broken template ships as a
 * failed deploy, never a silently unsent email.
 */
@Component
public class ClasspathTemplateCatalog implements TemplateCatalog {

    private static final Logger log = LoggerFactory.getLogger(ClasspathTemplateCatalog.class);

    private static final String BASE_PATH = "templates/email/";
    private static final List<String> ENGLISH_ONLY = List.of("en");

    /**
     * The declared template universe. The identity emails are English-only;
     * localized shopper emails arrive with their contexts.
     */
    private static final Map<String, List<String>> KNOWN_TEMPLATES = Map.of(
            "VERIFICATION_EMAIL", ENGLISH_ONLY,
            "PASSWORD_RESET_EMAIL", ENGLISH_ONLY,
            "PASSWORD_CHANGED_EMAIL", ENGLISH_ONLY,
            "ACCOUNT_ALREADY_REGISTERED_EMAIL", ENGLISH_ONLY);

    private final Map<String, EmailTemplate> templates;

    public ClasspathTemplateCatalog() {
        Map<String, EmailTemplate> loaded = new HashMap<>();
        KNOWN_TEMPLATES.forEach((type, locales) -> {
            for (String locale : locales) {
                loaded.put(key(type, locale), load(type, locale));
            }
        });
        this.templates = Map.copyOf(loaded);
        log.info("Loaded {} email templates from the classpath ({} types)",
                templates.size(), KNOWN_TEMPLATES.size());
    }

    @Override
    public Optional<EmailTemplate> find(String notificationType, String locale) {
        return Optional.ofNullable(templates.get(key(notificationType, locale)));
    }

    private static String key(String type, String locale) {
        return type + '#' + locale;
    }

    private static EmailTemplate load(String type, String locale) {
        String baseName = type.toLowerCase().replace('_', '-') + "_" + locale;
        String subject = read(BASE_PATH + baseName + ".subject.txt").strip();
        String body = read(BASE_PATH + baseName + ".html").strip();
        if (subject.isEmpty() || body.isEmpty()) {
            throw new IllegalStateException(
                    "Email template " + type + "/" + locale + " is blank ("
                            + BASE_PATH + baseName + ".*) — refusing to start");
        }
        return new EmailTemplate(locale, subject, body);
    }

    private static String read(String path) {
        ClassPathResource resource = new ClassPathResource(path);
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(
                    "Missing or unreadable email template file '" + path
                            + "' — refusing to start", e);
        }
    }
}
