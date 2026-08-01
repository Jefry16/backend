package com.vointika.notification.infrastructure.template;

import com.vointika.notification.application.port.TemplateCatalog;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The template set and the platform's UI languages are two lists that must agree,
 * and nothing used to make them.
 *
 * <p>{@code UiLanguageProperties} promises that growing a language is
 * configuration only. For email it is not: a language on the allowlist with no
 * template pair still *works* — {@code SendNotificationUseCase} falls back
 * exact → subtag → {@code en} — so a user who chose it receives **English**, and
 * nothing fails, warns or 500s. The degradation is invisible.
 *
 * <p>This test turns that into a build failure: add a code to
 * {@code app.identity.ui-languages} without shipping
 * {@code {type}_{code}.html} + {@code .subject.txt} and adding the code to
 * {@code ClasspathTemplateCatalog.LOCALES}, and it fails here rather than in a
 * stranger's inbox.
 */
class TemplateLocalesTrackUiLanguagesTest {

    private final ClasspathTemplateCatalog catalog = new ClasspathTemplateCatalog();

    /** Every notification type the catalog declares. */
    private static final List<String> TYPES = List.of(
            "VERIFICATION_EMAIL", "PASSWORD_RESET_EMAIL", "PASSWORD_CHANGED_EMAIL",
            "ACCOUNT_ALREADY_REGISTERED_EMAIL", "TOUR_OPERATOR_WELCOME_EMAIL",
            "TEAM_INVITATION_EMAIL");

    @Test
    void everyUiLanguageHasAFullSetOfEmailTemplates() throws IOException {
        List<String> uiLanguages = uiLanguagesFromApplicationYml();
        assertThat(uiLanguages).isNotEmpty();

        for (String language : uiLanguages) {
            for (String type : TYPES) {
                assertThat(catalog.find(type, language))
                        .withFailMessage(
                                "app.identity.ui-languages offers '%s' but there is no %s template for it. "
                                        + "Ship templates/email/%s_%s.{html,subject.txt} and add '%s' to "
                                        + "ClasspathTemplateCatalog.LOCALES — otherwise that user is emailed in English.",
                                language, type, type.toLowerCase().replace('_', '-'), language, language)
                        .isPresent();
            }
        }
    }

    /**
     * Reads the real {@code application.yml}. The value is
     * {@code ${APP_IDENTITY_UI_LANGUAGES:en,es}} — a placeholder with a default —
     * and there is no Environment here to resolve it, so the default is taken
     * literally. That is the right target: it is what a deployment ships with
     * unless someone overrides the variable.
     */
    private static List<String> uiLanguagesFromApplicationYml() throws IOException {
        PropertySource<?> yml = new YamlPropertySourceLoader()
                .load("application.yml", new ClassPathResource("application.yml"))
                .getFirst();
        String raw = String.valueOf(yml.getProperty("app.identity.ui-languages"));

        if (raw.startsWith("${") && raw.endsWith("}")) {
            String inner = raw.substring(2, raw.length() - 1);
            int colon = inner.indexOf(':');
            raw = colon < 0 ? "" : inner.substring(colon + 1);
        }
        return Arrays.stream(raw.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    @Test
    void aLanguageWithoutTemplatesWouldSilentlyFallBackToEnglish() {
        // The reason the test above has to exist: the catalog misses, and the use
        // case's exact → subtag → en chain then resolves the English template.
        assertThat(catalog.find("VERIFICATION_EMAIL", "fr")).isEmpty();
        assertThat(catalog.find("VERIFICATION_EMAIL", "en"))
                .map(TemplateCatalog.EmailTemplate::locale)
                .contains("en");
    }
}
