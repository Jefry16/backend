package com.vointika.notification.infrastructure.template;

import com.vointika.notification.application.port.NotificationType;
import com.vointika.notification.application.port.TemplateCatalog.EmailTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the REAL classpath templates (src/main/resources/templates/email/) — the
 * boot-time fail-fast means constructing the catalog IS the assertion that every
 * declared (type, locale) file pair exists and is non-blank. The identity emails
 * ship in every platform locale (en + es).
 */
class ClasspathTemplateCatalogTest {

    private final ClasspathTemplateCatalog catalog = new ClasspathTemplateCatalog();

    /** The declared universe, read from the enum rather than re-listed here. */
    private static final NotificationType[] TYPES = NotificationType.values();
    private static final List<String> LOCALES = List.of("en", "es");

    @Test
    void loadsEveryIdentityTemplateInEveryPlatformLocale() {
        for (NotificationType type : TYPES) {
            for (String locale : LOCALES) {
                EmailTemplate t = catalog.find(type, locale)
                        .orElseThrow(() -> new AssertionError(type + "/" + locale + " missing"));
                assertEquals(locale, t.locale());
            }
        }
    }

    @Test
    void lookupIsLocaleExact_noFallbackChainInsideTheCatalog() {
        // The exact→subtag→en chain lives in SendNotificationUseCase; the catalog
        // itself is a plain keyed read. A locale we don't ship, or an unknown type,
        // misses here.
        assertFalse(catalog.find(NotificationType.VERIFICATION_EMAIL, "fr").isPresent());
    }

    @Test
    void everyTemplateHasANonBlankSubjectAndAnHtmlBody() {
        for (NotificationType type : TYPES) {
            for (String locale : LOCALES) {
                EmailTemplate t = catalog.find(type, locale).orElseThrow();
                assertFalse(t.subject().isBlank(), type + "/" + locale + " subject");
                assertTrue(t.body().startsWith("<!DOCTYPE html>"), type + "/" + locale + " doctype");
                assertTrue(t.body().endsWith("</html>"), type + "/" + locale + " close");
            }
        }
    }
}
