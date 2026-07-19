package com.vointika.notification.infrastructure.template;

import com.vointika.notification.application.port.TemplateCatalog.EmailTemplate;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Loads the REAL classpath templates (src/main/resources/templates/email/) —
 * the boot-time fail-fast means constructing the catalog IS the assertion that
 * every declared file pair exists and is non-blank. This slice ships the four
 * English-only identity emails; localized shopper emails arrive with their
 * contexts.
 */
class ClasspathTemplateCatalogTest {

    private final ClasspathTemplateCatalog catalog = new ClasspathTemplateCatalog();

    private static final List<String> IDENTITY_TYPES = List.of(
            "VERIFICATION_EMAIL", "PASSWORD_RESET_EMAIL", "PASSWORD_CHANGED_EMAIL",
            "ACCOUNT_ALREADY_REGISTERED_EMAIL");

    @Test
    void loadsEveryIdentityTemplateInEnglish() {
        for (String type : IDENTITY_TYPES) {
            assertTrue(catalog.find(type, "en").isPresent(), type + "/en");
            assertEquals("en", catalog.find(type, "en").orElseThrow().locale());
        }
    }

    @Test
    void lookupIsLocaleExact_noChainInsideTheCatalog() {
        // The exact→subtag→en chain lives in SendNotificationUseCase; the catalog
        // itself is a plain keyed read. Identity templates are English-only, so a
        // non-en lookup misses here.
        assertFalse(catalog.find("VERIFICATION_EMAIL", "es").isPresent());
        assertFalse(catalog.find("UNKNOWN_TYPE", "en").isPresent());
    }

    @Test
    void everyTemplateHasANonBlankSubjectAndAThymeleafHtmlBody() {
        for (String type : IDENTITY_TYPES) {
            EmailTemplate t = catalog.find(type, "en").orElseThrow();
            assertFalse(t.subject().isBlank(), type + " subject");
            assertTrue(t.body().startsWith("<!DOCTYPE html>"), type + " body doctype");
            assertTrue(t.body().endsWith("</body></html>"), type + " body close");
        }
    }
}
