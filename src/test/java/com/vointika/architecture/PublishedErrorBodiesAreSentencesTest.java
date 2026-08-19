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
 * A message stubbed into a documentation test is published, so it must be a sentence.
 *
 * <p><b>What this catches.</b> {@code doThrow(new ResourceAlreadyExistsException("exists"))}
 * reads as throwaway scaffolding and is not: the guide then tells clients that a name
 * clash answers {@code {"message": "exists"}}. Four such bodies shipped — {@code "admin"}
 * twice, {@code "exists"}, {@code "not found"} — across {@code pickup} and
 * {@code experience}. None is producible, so a client matching on {@code message} never
 * matched.
 *
 * <p><b>The rule is a proxy, deliberately.</b> "Starts with a capital" is not the real
 * requirement — "is what production actually throws" is — but that cannot be checked
 * mechanically here, because several real messages are <em>interpolated</em> and so
 * appear nowhere in {@code src/main} verbatim: the ADMIN 403 is
 * {@code "This action requires " + minimum + " privileges"}, and there are three more
 * like it. A verbatim-in-{@code src/main} check flags all of those and is unusable.
 *
 * <p>Every genuine message in this repository is a sentence and every placeholder was a
 * bare lower-case token, so the initial capital separates them exactly: it flagged the
 * four real offenders and nothing else. <b>It will not catch a plausible-looking wrong
 * sentence</b> — replacing a placeholder with the wrong real constant published
 * "Audience not found" for a non-member and this guard would have passed it. For that,
 * `PATTERNS.md` §9a: check which throw the stub stands in for, not which noun the
 * endpoint is about.
 */
class PublishedErrorBodiesAreSentencesTest {

    /** A string literal handed straight to an exception constructor. */
    private static final Pattern STUBBED_MESSAGE =
            Pattern.compile("new\\s+\\w*(?:Exception|Error)\\s*\\(\\s*\"([^\"\\\\\\n]*)\"");

    @Test
    @DisplayName("a documentation test never publishes a placeholder for an error body")
    void everyStubbedMessageIsASentence() throws IOException {
        List<String> offenders = new ArrayList<>();
        int scannedFiles = 0;
        int scannedMessages = 0;

        try (Stream<Path> files = Files.walk(Path.of("src", "test", "java"))) {
            for (Path file : files.filter(p -> p.toString().endsWith("DocumentationTest.java")).toList()) {
                scannedFiles++;
                Matcher matcher = STUBBED_MESSAGE.matcher(Files.readString(file));
                while (matcher.find()) {
                    scannedMessages++;
                    String message = matcher.group(1);
                    if (!message.isEmpty() && !Character.isUpperCase(message.charAt(0))) {
                        offenders.add("\"" + message + "\" in " + file);
                    }
                }
            }
        }

        assertThat(scannedFiles)
                .withFailMessage("Walked %d *DocumentationTest.java files — the scan found "
                        + "nothing, so this assertion proves nothing. Check the root.", scannedFiles)
                .isGreaterThan(20);
        assertThat(scannedMessages)
                .withFailMessage("Found %d stubbed exception messages across %d files. Zero would "
                        + "mean the pattern stopped matching, not that the repo is clean.",
                        scannedMessages, scannedFiles)
                .isGreaterThan(15);

        assertThat(offenders)
                .withFailMessage("""
                        %d documentation test(s) stub an error with a placeholder rather than \
                        the message production throws:
                        %s
                        That string is not scaffolding — it is published as the endpoint's error \
                        body, so the API guide ends up promising a body no request can produce \
                        and a client matching on `message` never matches.
                        Stub the constant where one exists (TourOperatorMembershipCheck.TENANT_NOT_FOUND, \
                        <Ctx>Repository.NAME_TAKEN, <Noun>OwnershipQuery.NOT_FOUND), else the real \
                        sentence. Check which throw the stub stands in for, not which noun the \
                        endpoint is about — PATTERNS §9a.""",
                        offenders.size(), String.join("\n", offenders))
                .isEmpty();
    }
}
