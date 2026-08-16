package com.vointika.architecture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A documented error must not publish the same request as the success it contrasts
 * with.
 *
 * <p><b>Why this exists.</b> An error operation is written by copying the happy-path
 * test and changing the stub, so the two end up issuing byte-identical requests. The
 * guide then shows one call under "Get a Contact Message" returning 200 and the same
 * call under "— Not Found" returning 404, with nothing to say what differs — while the
 * path-parameter description claims the id does not exist. It has happened twice, on
 * `contact` and again on `pickup`, and neither the suite nor
 * {@code ApiGuideDocumentsEveryEndpointTest} can see it: both requests are valid and
 * both operations are referenced.
 *
 * <p>The rule is only about the <em>request</em>. Two operations may legitimately
 * share a URL when the difference is the caller rather than the call — a 403 turns on
 * who is asking — so an `Authorization` header that differs is enough to satisfy this.
 *
 * <p>It reads the generated snippets, so it needs them on disk. When they are absent
 * (a bare {@code test} run rather than {@code package}) it skips rather than failing,
 * because a missing directory is a build-phase fact and not a drift.
 */
class PublishedExamplesAreHonestTest {

    private static final Path SNIPPETS = Path.of("target", "generated-snippets");

    /** An operation whose last segment starts with one of these contrasts with its base. */
    private static final List<String> ERROR_MARKERS =
            List.of("-not-found", "-forbidden", "-conflict", "-unsupported", "-missing", "-empty", "-too-large");

    @Test
    @DisplayName("a documented error publishes a different request from its happy path")
    void errorExamplesDifferFromTheirHappyPath() throws IOException {
        if (!Files.isDirectory(SNIPPETS)) {
            return;
        }
        List<String> identical = new ArrayList<>();
        try (Stream<Path> dirs = Files.walk(SNIPPETS)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) {
                Path request = dir.resolve("http-request.adoc");
                if (!Files.isRegularFile(request)) {
                    continue;
                }
                String name = dir.getFileName().toString();
                String base = ERROR_MARKERS.stream().filter(name::contains)
                        .findFirst().map(m -> name.substring(0, name.indexOf(m))).orElse(null);
                if (base == null || base.isEmpty()) {
                    continue;
                }
                Path sibling = dir.resolveSibling(base).resolve("http-request.adoc");
                if (!Files.isRegularFile(sibling)) {
                    continue;
                }
                if (Files.readString(request).equals(Files.readString(sibling))) {
                    identical.add(SNIPPETS.relativize(dir).toString()
                            + " publishes the same request as " + base);
                }
            }
        }
        assertThat(identical)
                .withFailMessage("""
                        These error operations publish a request byte-identical to the success \
                        they contrast with, so the guide shows one call with two outcomes and \
                        nothing to say what changed:
                        %s
                        Vary the thing the error actually turns on — a missing id for a 404, a \
                        different token for a 403.""", String.join("\n", identical))
                .isEmpty();
    }
}
