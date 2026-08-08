package com.vointika.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every documented endpoint appears in the API guide, and the guide references
 * nothing that no longer exists.
 *
 * <p><b>Why this test exists.</b> Spring REST Docs takes two steps and only the
 * first was enforced: a controller test calls {@code document("pages/create")}
 * and emits snippets, and then {@code api-guide.adoc} has to pull them in with
 * an {@code operation::pages/create[]} line. Nothing checked the second. The
 * result was silent in both directions — the guide drifted to <b>46 documented
 * operations against 135 generated ones</b>, and separately kept two
 * {@code operation::} lines for endpoints that had been deleted, which
 * Asciidoctor renders without complaint. Missing docs looked like passing tests;
 * deleted endpoints looked like real ones.
 *
 * <p><b>It reads the test sources, not the generated snippets.</b> Snippets are
 * written by the other tests in this same run, so a snippet-based check would
 * depend on execution order and fail at random. The {@code document("...")}
 * literal in the source is the same fact, available before anything runs.
 *
 * <p>Fixing a failure is one line in {@code api-guide.adoc} per endpoint — plus
 * a sentence saying what it does, which is the part worth writing.
 */
class ApiGuideDocumentsEveryEndpointTest {

    private static final Path API_GUIDE = Path.of("src/docs/asciidoc/api-guide.adoc");
    private static final Path TEST_SOURCES = Path.of("src/test/java");

    /** {@code document("some/operation"} — the first argument is always a literal. */
    private static final Pattern DOCUMENT_CALL = Pattern.compile("document\\(\\s*\"([^\"]+)\"");
    private static final Pattern OPERATION_LINE = Pattern.compile("operation::([^\\[]+)\\[");

    @Test
    void theGuideAndTheDocumentationTestsDescribeTheSameEndpoints() throws IOException {
        Set<String> documented = operationsEmittedByTests();
        Set<String> inGuide = operationsReferencedByTheGuide();

        Set<String> missingFromGuide = new TreeSet<>(documented);
        missingFromGuide.removeAll(inGuide);
        Set<String> danglingInGuide = new TreeSet<>(inGuide);
        danglingInGuide.removeAll(documented);

        assertThat(missingFromGuide)
                .withFailMessage("""
                        %d endpoint(s) emit REST Docs snippets that api-guide.adoc never includes, \
                        so they are absent from the published guide:
                        %s
                        Add an `operation::<name>[]` line for each, under a heading that says what \
                        it does and who may call it.""",
                        missingFromGuide.size(), bullets(missingFromGuide))
                .isEmpty();

        assertThat(danglingInGuide)
                .withFailMessage("""
                        api-guide.adoc references %d operation(s) that no test emits — most likely \
                        endpoints that were deleted:
                        %s
                        Asciidoctor does NOT fail on a missing include, so the published HTML shows \
                        these as real endpoints. Remove their sections.""",
                        danglingInGuide.size(), bullets(danglingInGuide))
                .isEmpty();
    }

    /**
     * A duplicate {@code operation::} line renders the same endpoint twice and is
     * invisible to the set comparison above, which is why it is checked apart.
     */
    @Test
    void theGuideReferencesEachOperationExactlyOnce() throws IOException {
        List<String> all = allMatches(OPERATION_LINE, Files.readString(API_GUIDE, StandardCharsets.UTF_8));
        Set<String> duplicated = new TreeSet<>();
        Set<String> seen = new TreeSet<>();
        all.forEach(op -> {
            if (!seen.add(op)) {
                duplicated.add(op);
            }
        });

        assertThat(duplicated)
                .withFailMessage("api-guide.adoc includes these operations more than once:%n%s",
                        bullets(duplicated))
                .isEmpty();
    }

    private static Set<String> operationsEmittedByTests() throws IOException {
        try (Stream<Path> files = Files.walk(TEST_SOURCES)) {
            Set<String> found = new TreeSet<>();
            for (Path file : files.filter(f -> f.toString().endsWith("DocumentationTest.java")).toList()) {
                found.addAll(allMatches(DOCUMENT_CALL, Files.readString(file, StandardCharsets.UTF_8)));
            }
            // Non-vacuity: if the walk finds nothing, this test would pass while
            // checking nothing at all.
            assertThat(found)
                    .withFailMessage("Found no document(\"…\") calls under %s. This test cannot see "
                            + "what it exists to check — fix the test, do not delete it.", TEST_SOURCES)
                    .isNotEmpty();
            return found;
        }
    }

    private static Set<String> operationsReferencedByTheGuide() throws IOException {
        assertThat(Files.exists(API_GUIDE))
                .withFailMessage("%s not found. Tests run from the project root; if the guide moved, "
                        + "point this test at it.", API_GUIDE)
                .isTrue();
        return new TreeSet<>(allMatches(OPERATION_LINE,
                Files.readString(API_GUIDE, StandardCharsets.UTF_8)));
    }

    private static List<String> allMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        return matcher.results().map(r -> r.group(1).trim()).toList();
    }

    private static String bullets(Set<String> items) {
        return items.stream().map(i -> "  - " + i).reduce((a, b) -> a + "\n" + b).orElse("");
    }
}
