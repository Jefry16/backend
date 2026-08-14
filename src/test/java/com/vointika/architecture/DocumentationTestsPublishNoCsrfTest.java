package com.vointika.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A documentation test never applies MockMvc's CSRF post-processor, because
 * whatever it sends is <b>published</b>.
 *
 * <p><b>Why this test exists.</b> {@code SecurityConfig} disables CSRF, so
 * {@code .with(csrf())} changes no behaviour and every test using it passed. What
 * it did change was the guide: MockMvc attaches the token to the request, and
 * REST Docs writes the request out verbatim. 52 of 153 operations published a
 * {@code _csrf} parameter the API does not accept — as a query parameter on JSON
 * endpoints ({@code POST /api/…/pages?_csrf=…}) and, on a {@code DELETE}, as an
 * invented {@code application/x-www-form-urlencoded} body. It reached the
 * copy-pasteable curl examples and the rendered HTML.
 *
 * <p>Nothing detected it for the same reason it was easy to write: the call is a
 * no-op, so the suite stays green either way, and the drift is only visible in
 * generated output nobody diffs. The guide even documented the same operation
 * two ways — {@code experiences/create} clean, {@code pages/create} polluted.
 *
 * <p><b>It reads the test sources, not the generated snippets.</b> Snippets are
 * written by the other tests in this same run, so a snippet-based check would
 * depend on execution order and fail at random — the reasoning
 * {@link ApiGuideDocumentsEveryEndpointTest} already records.
 *
 * <p>If a documentation test ever genuinely needs CSRF enforced, it is
 * documenting a security posture the application does not have; fix
 * {@code SecurityConfig} or the test, not this rule.
 */
class DocumentationTestsPublishNoCsrfTest {

    private static final Path TEST_SOURCES = Path.of("src/test/java");

    /**
     * The call anywhere on a code line — deliberately <b>not</b> anchored to
     * {@code .with(}. Anchoring it looked more precise and silently stopped
     * detecting: a formatter breaking the long line into {@code .with(} +
     * {@code csrf())} put the two halves on different lines, and a per-line
     * matcher saw neither. It also missed {@code var c = csrf();} used later.
     * Comment lines are already filtered out, so the bare call is safe to match
     * and is the broader net.
     */
    private static final Pattern CSRF_CALL = Pattern.compile("\\bcsrf\\s*\\(");

    @Test
    void noDocumentationTestAppliesTheCsrfPostProcessor() throws IOException {
        Set<String> offenders = new TreeSet<>();
        int examined = 0;

        try (Stream<Path> files = Files.walk(TEST_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith("DocumentationTest.java")).toList()) {
                examined++;
                if (appliesCsrf(Files.readString(file, StandardCharsets.UTF_8))) {
                    offenders.add(TEST_SOURCES.relativize(file).toString());
                }
            }
        }

        // Non-vacuity: a walk that finds no documentation tests would pass while
        // checking nothing at all. A count threshold was tempting and wrong — it
        // fails on legitimate consolidation, and the `*DocumentationTest.java`
        // convention both tests depend on is already guarded by the sibling's own
        // non-vacuity check, which goes quiet at the same moment this one would.
        assertThat(examined)
                .withFailMessage("Found no *DocumentationTest.java under %s. This test cannot see "
                        + "what it exists to check — fix the test, do not delete it.", TEST_SOURCES)
                .isNotZero();

        assertThat(offenders)
                .withFailMessage("""
                        %d documentation test(s) apply MockMvc's CSRF post-processor:
                        %s
                        SecurityConfig disables CSRF, so this changes no behaviour — but REST Docs \
                        publishes the token, and the guide then tells consumers to send a `_csrf` \
                        parameter the API does not accept. Delete the `.with(csrf())` call and its \
                        static import.""",
                        offenders.size(), bullets(offenders))
                .isEmpty();
    }

    /**
     * <b>Comment lines are skipped, and that is the whole subtlety here.</b> The
     * first version matched the raw source, so the most natural comment anyone
     * would write — "deliberately no {@code .with(csrf())}" — failed the build. A
     * guard that punishes you for explaining it is worse than no guard.
     *
     * <p>Lines are filtered rather than comments stripped, because stripping
     * {@code //} to end-of-line would cut a line containing {@code "http://…"} in
     * a test fixture and hide real code behind it — a false negative, which is the
     * failure this test cannot afford. A trailing {@code // .with(csrf())} after
     * live code still trips it; that is commented-out code, which LAW §6.3 bans.
     */
    private static boolean appliesCsrf(String source) {
        return source.lines()
                .map(String::strip)
                .filter(line -> !line.startsWith("*") && !line.startsWith("//") && !line.startsWith("/*"))
                .anyMatch(line -> CSRF_CALL.matcher(line).find());
    }

    private static String bullets(Set<String> items) {
        return items.stream().map(i -> "  - " + i).reduce((a, b) -> a + "\n" + b).orElse("");
    }
}
