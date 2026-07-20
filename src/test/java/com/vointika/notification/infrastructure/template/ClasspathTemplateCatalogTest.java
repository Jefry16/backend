package com.vointika.notification.infrastructure.template;

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

    private static final List<String> TYPES = List.of(
            "VERIFICATION_EMAIL", "PASSWORD_RESET_EMAIL", "PASSWORD_CHANGED_EMAIL",
            "ACCOUNT_ALREADY_REGISTERED_EMAIL", "TOUR_OPERATOR_WELCOME_EMAIL",
            "TEAM_INVITATION_EMAIL");
    private static final List<String> LOCALES = List.of("en", "es");

    @Test
    void loadsEveryIdentityTemplateInEveryPlatformLocale() {
        for (String type : TYPES) {
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
        assertFalse(catalog.find("VERIFICATION_EMAIL", "fr").isPresent());
        assertFalse(catalog.find("UNKNOWN_TYPE", "en").isPresent());
    }

    @Test
    void everyTemplateHasANonBlankSubjectAndAnHtmlBody() {
        for (String type : TYPES) {
            for (String locale : LOCALES) {
                EmailTemplate t = catalog.find(type, locale).orElseThrow();
                assertFalse(t.subject().isBlank(), type + "/" + locale + " subject");
                assertTrue(t.body().startsWith("<!DOCTYPE html>"), type + "/" + locale + " doctype");
                assertTrue(t.body().endsWith("</html>"), type + "/" + locale + " close");
            }
        }
    }
}
