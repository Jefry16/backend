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
 * clash answers {@code {"message": "exists"}}. Five such bodies shipped — {@code "admin"}
 * twice, {@code "exists"}, {@code "not found"} twice — across {@code pickup} and
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
 * <p><b>Every message that reaches a client is a sentence</b> — that, not "every message
 * in the repository", is the claim this rests on, and three production messages are
 * lower-case ({@code ListSchema:123}, {@code SortSpec:24}, {@code JwtProperties:24}).
 * All three are {@code IllegalState}/{@code IllegalArgument} raised at wiring or parse
 * time, never mapped to a response body and never stubbed in a documentation test, so
 * the scan root keeps them out of reach. Placeholders were bare lower-case tokens, so
 * the initial capital separates them exactly.
 *
 * <p><b>What it cannot see.</b> A plausible-looking wrong sentence passes — replacing a
 * placeholder with the wrong real constant published "Audience not found" for a
 * non-member, and this guard would have waved it through; for that, `PATTERNS.md` §9a,
 * check which throw the stub stands in for, not which noun the endpoint is about.
 *
 * <p><b>What looks like a second hole is closed by the exception API, not by luck.</b>
 * The pattern only reads the constructor's first argument — but all thirteen
 * constructors in {@code shared.exception} take {@code String message} first, so
 * {@code new InvalidFieldException(field, "message")} is not a shape this hierarchy
 * admits. That is the property to preserve: adding a constructor that puts anything
 * before the message is what would reopen it. ("Nothing does that today" was the earlier
 * wording, and it was checked against call sites rather than signatures — which reads as
 * a standing gap and invites widening the regex to close a hole that is not there.)
 */
class PublishedErrorBodiesAreSentencesTest {

    /**
     * A string literal handed straight to an exception constructor.
     *
     * <p><b>The character class must allow {@code .}</b>, or a fully-qualified
     * constructor is invisible: {@code \w} is {@code [a-zA-Z0-9_]}. The first version of
     * this guard used {@code \w*} and reported the repository clean while
     * {@code new com.vointika.shared.exception.ResourceNotFoundException("not found")}
     * was still publishing {@code slots/list-not-found}.
     *
     * <p><b>It was blind to four stub sites, and only one of them happened to hold a
     * placeholder</b> — the other three carried real sentences, so nothing else was
     * published wrong by luck rather than by structure. <b>The census that claimed "four
     * offenders" was taken with this same pattern, so it inherited the blind spot it was
     * measuring with.</b> A guard used to sweep for its own class cannot also be the
     * evidence that the class is empty: plant the form you doubt and watch it fail.
     */
    private static final Pattern STUBBED_MESSAGE =
            Pattern.compile("new\\s+[\\w.]*(?:Exception|Error)\\s*\\(\\s*\"([^\"\\\\\\n]*)\"");

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
