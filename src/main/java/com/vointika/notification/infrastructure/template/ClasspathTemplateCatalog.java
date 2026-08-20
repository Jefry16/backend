package com.vointika.notification.infrastructure.template;

import com.vointika.notification.application.port.NotificationType;
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

    /**
     * Locales every identity email ships in. These <b>must</b> track the
     * platform's UI languages ({@code app.identity.ui-languages}) and
     * {@code TemplateLocalesTrackUiLanguagesTest} fails the build when they do
     * not — a language on the allowlist with no templates is the one failure
     * mode nothing else catches, because the send succeeds in English instead.
     * Growing a language means adding its code here AND the matching
     * {@code {type}_{locale}} files, or the fail-fast loader below refuses to
     * start.
     */
    private static final List<String> LOCALES = List.of("en", "es");

    /**
     * The declared universe is {@link NotificationType#values()}. It used to be a
     * {@code KNOWN_TEMPLATES} map here, every entry pointing at this same locale list —
     * so the map's only job was naming the types, which is what the enum does.
     */
    private final Map<String, EmailTemplate> templates;

    public ClasspathTemplateCatalog() {
        Map<String, EmailTemplate> loaded = new HashMap<>();
        for (NotificationType type : NotificationType.values()) {
            for (String locale : LOCALES) {
                loaded.put(key(type, locale), load(type, locale));
            }
        }
        this.templates = Map.copyOf(loaded);
        log.info("Loaded {} email templates from the classpath ({} types)",
                templates.size(), NotificationType.values().length);
    }

    @Override
    public Optional<EmailTemplate> find(NotificationType notificationType, String locale) {
        return Optional.ofNullable(templates.get(key(notificationType, locale)));
    }

    private static String key(NotificationType type, String locale) {
        return type.name() + '#' + locale;
    }

    private static EmailTemplate load(NotificationType type, String locale) {
        String baseName = type.fileBase() + "_" + locale;
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
