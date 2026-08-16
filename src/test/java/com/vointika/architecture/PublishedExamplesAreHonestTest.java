package com.vointika.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A documented error must not publish the same request as the success it contrasts
 * with.
 *
 * <p><b>Why this exists.</b> An error operation is written by copying the happy-path
 * test and changing the stub, so the two end up issuing byte-identical requests. The
 * guide then shows one call under "Get a Contact Message" returning 200 and the same
 * call under "— Not Found" returning 404, with nothing to say what differs — while
 * the path-parameter description claims the id does not exist. On its first run this
 * found <b>ten</b> such pairs across five contexts, including two that had been fixed
 * by hand in one context and then reproduced in four others. Neither the suite nor
 * {@link ApiGuideDocumentsEveryEndpointTest} can see it: both requests are valid and
 * both operations are referenced.
 *
 * <p>The rule is only about the <em>request</em>. Two operations may legitimately
 * share a URL when the difference is the caller rather than the call — a 403 turns on
 * who is asking — so an {@code Authorization} header that differs is enough.
 *
 * <p><b>An error is found by its status, not by its name.</b> An earlier version
 * matched a hand-kept list of name fragments ({@code -not-found}, {@code -forbidden},
 * …), which is the same restatement this guard exists to remove: the next section
 * called {@code -gone} or {@code -too-many} would have been silently unguarded. The
 * status line in {@code http-response.adoc} carries the fact already, so that is what
 * is read, and the happy path is the longest sibling operation whose name is a prefix
 * of the error's.
 */
class PublishedExamplesAreHonestTest {

    private static final Path SNIPPETS = Path.of("target", "generated-snippets");

    /** {@code HTTP/1.1 403 Forbidden} — the first line inside the response listing. */
    private static final Pattern STATUS = Pattern.compile("HTTP/\\d\\.\\d (\\d{3})");

    /**
     * Well under the real count, and only here to tell "nothing on disk" apart from
     * "nothing wrong". A vacuous pass is worse than no guard (`CLAUDE.md`), and this
     * test reads generated output, so it would otherwise report success against an
     * empty tree or — if surefire ever reordered it — against a partial one.
     */
    private static final int MINIMUM_OPERATIONS = 100;

    @Test
    @DisplayName("a documented error publishes a different request from its happy path")
    void errorExamplesDifferFromTheirHappyPath() throws IOException {
        Map<String, Path> operations = operations();

        assertThat(operations)
                .withFailMessage("""
                        Found %d operation directories under %s, expected at least %d. \
                        Either the snippets were never generated — run `./mvnw package`, \
                        not `test`, because asciidoctor is bound to prepare-package — or \
                        this test ran before the documentation tests and would have \
                        checked only part of the surface.""",
                        operations.size(), SNIPPETS, MINIMUM_OPERATIONS)
                .hasSizeGreaterThan(MINIMUM_OPERATIONS);

        List<String> identical = new ArrayList<>();
        for (Map.Entry<String, Path> entry : operations.entrySet()) {
            String name = entry.getKey();
            if (statusOf(entry.getValue()) < 400) {
                continue;
            }
            String happyPath = longestPrefixOperation(name, operations);
            if (happyPath == null) {
                continue;
            }
            Path errorRequest = entry.getValue().resolve("http-request.adoc");
            Path happyRequest = operations.get(happyPath).resolve("http-request.adoc");
            if (Files.isRegularFile(errorRequest) && Files.isRegularFile(happyRequest)
                    && Files.readString(errorRequest).equals(Files.readString(happyRequest))) {
                identical.add(name + " publishes the same request as " + happyPath);
            }
        }

        assertThat(identical)
                .withFailMessage("""
                        These error operations publish a request byte-identical to the \
                        success they contrast with, so the guide shows one call with two \
                        outcomes and nothing to say what changed:
                        %s
                        Vary the thing the error actually turns on — a missing id for a \
                        404, a different token for a 403, the clashing value for a 409.""",
                        String.join("\n", identical))
                .isEmpty();
    }

    /** Every operation directory, keyed by its {@code operation::} name. */
    private static Map<String, Path> operations() throws IOException {
        Map<String, Path> found = new LinkedHashMap<>();
        if (!Files.isDirectory(SNIPPETS)) {
            return found;
        }
        try (Stream<Path> tree = Files.walk(SNIPPETS)) {
            tree.filter(p -> Files.isRegularFile(p.resolve("http-response.adoc")))
                    .forEach(p -> found.put(SNIPPETS.relativize(p).toString().replace('\\', '/'), p));
        }
        return found;
    }

    private static int statusOf(Path operation) throws IOException {
        Matcher m = STATUS.matcher(Files.readString(operation.resolve("http-response.adoc")));
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    /**
     * The success this error contrasts with: the longest other operation whose name is
     * a prefix of it. {@code media/upload-forbidden} finds {@code media/upload};
     * {@code authentication/unauthorized} finds nothing, correctly — it has no happy
     * path to differ from.
     */
    private static String longestPrefixOperation(String errorName, Map<String, Path> operations) {
        return operations.keySet().stream()
                .filter(candidate -> !candidate.equals(errorName))
                .filter(errorName::startsWith)
                .max(java.util.Comparator.comparingInt(String::length))
                .orElse(null);
    }
}
