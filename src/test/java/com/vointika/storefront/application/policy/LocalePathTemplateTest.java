package com.vointika.storefront.application.policy;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The content-language allowlist and the storefront's URL shape are two lists
 * that must agree, and constraining {@link LocaleResolver#PATH_TEMPLATE} is what
 * made them two.
 *
 * <p>The constraint exists because the same string is registered as a
 * {@code PublicRoute}, where a bare {@code /{locale}} opens every single-segment
 * path. The cost is this coupling: seed a code the pattern does not match —
 * {@code zh-Hant}, say, or anything with an uppercase letter — and the operator
 * can select it, translate into it, and have the storefront answer <b>404</b>
 * for a locale it publishes. Nothing else would fail.
 *
 * <p>Same shape as {@code TemplateLocalesTrackUiLanguagesTest}: a second list
 * that must track an allowlist needs a build failure, not a comment saying it
 * should.
 */
class LocalePathTemplateTest {

    /** `(id, code, name)` tuples in the reference migrations. */
    private static final Pattern SEEDED_CODE =
            Pattern.compile("\\('[0-9a-fA-F-]{36}',\\s*'([^']+)'");

    /**
     * The bare regex out of {@code /&#123;locale:<regex>&#125;}, and the assertion
     * that there is one at all — an unconstrained variable is the whole defect
     * this file exists for, and it should say so rather than die parsing.
     */
    private static Pattern localeRegex() {
        String template = LocaleResolver.PATH_TEMPLATE;
        int constraint = template.indexOf(':');
        assertThat(constraint)
                .withFailMessage("LocaleResolver.PATH_TEMPLATE is '%s'. An unconstrained {locale} is also "
                        + "the PublicRoute pattern, so it makes every single-segment path in the "
                        + "application public — /error, /favicon.ico, and any /health or /metrics added "
                        + "later — and it pulls the storefront's gate over the container's error "
                        + "dispatch. Constrain the variable.", template)
                .isNotNegative();
        return Pattern.compile(template.substring(constraint + 1, template.length() - 1));
    }

    @Test
    void everySeededLanguageCodeIsAddressableAsAUrlPrefix() throws IOException {
        Pattern locale = localeRegex();
        List<String> codes = seededLanguageCodes();
        assertThat(codes).isNotEmpty();

        for (String code : codes) {
            assertThat(locale.matcher(code).matches())
                    .withFailMessage(
                            "reference.languages seeds '%s' but LocaleResolver.PATH_TEMPLATE does not match it, "
                                    + "so an operator publishing that locale would get a 404 at /%s and nothing "
                                    + "would say why. Widen the template — and remember it is also the "
                                    + "PublicRoute pattern, so widening it to a bare {locale} opens every "
                                    + "single-segment path.",
                            code, code)
                    .isTrue();
        }
    }

    /**
     * A locale is exactly one path segment, so the template must not swallow a
     * deeper path — otherwise the security pattern is wider than it reads.
     */
    @Test
    void theTemplateMatchesNeitherASlashNorAnEmptySegment() {
        Pattern locale = localeRegex();

        assertThat(locale.matcher("en/pages").matches()).isFalse();
        assertThat(locale.matcher("").matches()).isFalse();
    }

    /**
     * The storefront's top-level literal routes must stay out of the template's
     * reach. Eleven characters do not match two today, so {@code /experiences}
     * routes to its own controller — but that is a property of the constraint,
     * not of the words, and loosening the template to a bare {@code &#123;locale&#125;}
     * would make every one of them a locale prefix instead.
     */
    @Test
    void theTemplateDoesNotSwallowTheStorefrontsOwnLiteralRoutes() {
        Pattern locale = localeRegex();

        assertThat(locale.matcher("experiences").matches()).isFalse();
        assertThat(locale.matcher("password").matches()).isFalse();
    }

    /** Reads the real migrations, so adding a language in a new `V` is covered without editing this test. */
    private static List<String> seededLanguageCodes() throws IOException {
        File[] migrations = new ClassPathResource("db/migration/reference").getFile().listFiles();
        assertThat(migrations).isNotNull();

        List<String> codes = new ArrayList<>();
        for (File migration : migrations) {
            String sql = Files.readString(migration.toPath(), StandardCharsets.UTF_8);
            if (!sql.contains("INSERT INTO reference.languages")) {
                continue;
            }
            Matcher matcher = SEEDED_CODE.matcher(sql.substring(sql.indexOf("INSERT INTO reference.languages")));
            while (matcher.find()) {
                codes.add(matcher.group(1));
            }
        }
        return codes;
    }
}
