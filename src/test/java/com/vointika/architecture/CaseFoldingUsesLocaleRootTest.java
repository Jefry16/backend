package com.vointika.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * No production code case-folds with the JVM's default locale.
 *
 * <p>`PATTERNS.md` §11 has said this for a long time and has been broken in <b>four</b>
 * places: {@code LocaleResolver} and {@code TenantHandleResolver}, fixed earlier, then
 * {@code NotificationType.fileBase()} and {@code RequestSizeLimitFilter} — the second of
 * which had been sitting there and was only found by sweeping for the first. A written
 * rule broken four times is a rule that wants a build gate.
 *
 * <p><b>Why it matters.</b> Turkish folds <b>uppercase</b> {@code I} to a dotless
 * {@code ı} — a lowercase {@code i} passes through untouched — so on a host whose default
 * locale is {@code tr} the same string can produce different text, invisibly to every
 * machine anyone develops or runs CI on. The two live cases are <b>different kinds of
 * exposure</b>, which is the useful thing to carry to the next one:
 * <ul>
 *   <li><b>Certain, by construction.</b> {@code "VERIFICATION_EMAIL".toLowerCase()} →
 *       {@code verıfıcatıon-emaıl}. The value is {@code SCREAMING_CASE} by definition, so
 *       every {@code I} in it is uppercase and no caller can pick a casing that avoids it.
 *       All six constants fold; the eager catalog loader then refuses to start.</li>
 *   <li><b>Conditional, on what a caller sends.</b> {@code "MULTIPART/FORM-DATA"} →
 *       {@code multıpart/...} and the size-limit filter stops recognising the upload, which
 *       then gets the JSON cap. {@code "Multipart/form-data"} is <em>fine</em> — its
 *       {@code i} is already lowercase. RFC 9110 makes media types case-insensitive, so the
 *       all-caps form is legal to receive and cannot be assumed away.</li>
 * </ul>
 *
 * <p><b>Scope is {@code src/main} only.</b> A test folding case in a fixture is not
 * shipping anywhere, and widening this to {@code src/test} buys nothing but noise.
 *
 * <p>The fix is always the same three characters: {@code toLowerCase(Locale.ROOT)}.
 * {@code Locale.ROOT} — not {@code Locale.ENGLISH}, which is a language choice where this
 * is a "do not apply any language's rules" choice.
 */
class CaseFoldingUsesLocaleRootTest {

    /** {@code toLowerCase()} / {@code toUpperCase()} with no argument — the default locale. */
    private static final Pattern DEFAULT_LOCALE_FOLD =
            Pattern.compile("\\.to(?:Lower|Upper)Case\\(\\s*\\)");

    @Test
    @DisplayName("production code never case-folds with the default locale")
    void everyCaseFoldNamesItsLocale() throws IOException {
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        try (Stream<Path> files = Files.walk(Path.of("src", "main", "java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                scanned++;
                String source = Files.readString(file);
                Matcher matcher = DEFAULT_LOCALE_FOLD.matcher(source);
                while (matcher.find()) {
                    // A mention inside a comment is the rule being explained, not broken.
                    if (!isInsideComment(source, matcher.start())) {
                        offenders.add(file + " — " + lineAt(source, matcher.start()));
                    }
                }
            }
        }

        assertThat(scanned)
                .withFailMessage("Walked %d production files — the scan found nothing, so this "
                        + "assertion proves nothing. Check the root.", scanned)
                .isGreaterThan(500);

        assertThat(offenders)
                .withFailMessage("""
                        %d case-fold(s) use the JVM default locale:
                        %s
                        Under a Turkish default, UPPERCASE I folds to a dotless ı (lowercase i \
                        is untouched) — so the same string produces different text on a host whose \
                        locale you did not choose, and nothing on your machine or in CI will show \
                        it. Use toLowerCase(Locale.ROOT) (PATTERNS §11).""", offenders.size(), String.join("\n", offenders))
                .isEmpty();
    }

    /** Crude but sufficient: is this offset inside a {@code //} line or a {@code *} javadoc line? */
    private static boolean isInsideComment(String source, int offset) {
        int lineStart = source.lastIndexOf('\n', offset) + 1;
        String beforeOnLine = source.substring(lineStart, offset).stripLeading();
        return beforeOnLine.startsWith("//") || beforeOnLine.startsWith("*");
    }

    private static String lineAt(String source, int offset) {
        int start = source.lastIndexOf('\n', offset) + 1;
        int end = source.indexOf('\n', offset);
        return source.substring(start, end < 0 ? source.length() : end).strip();
    }
}
