package com.vointika.architecture;

import com.vointika.media.application.usecase.UploadMediaUseCase;
import com.vointika.media.domain.valueobject.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The API guide's prose names exactly the content types the media library accepts.
 *
 * <p><b>Why this exists.</b> The upload's field table is generated from
 * {@link ContentType#ALLOWED}, so it cannot drift — but the guide's hand-written prose
 * describes the same set twice, and that half was unguarded. Adding {@code image/avif}
 * to the allowlist was demonstrated to leave a green build with the table listing five
 * types and the prose still asserting "exactly four types" two paragraphs down. That is
 * the same defect the media pass reported, with the sides swapped: first the prose was
 * right and the contract wrong, then the contract right and the prose wrong.
 *
 * <p>This closes the second direction. It reads the prose rather than the snippets, so
 * it needs no generated output and runs in any phase.
 *
 * <p>It deliberately checks <b>both ways</b>. A type in the allowlist and not in the
 * guide is an undocumented capability; a type in the guide and not in the allowlist is
 * a promise the API breaks — the one that sends a client to upload an SVG.
 *
 * <p><b>Scope is the allowlist sentence, not the section.</b> Two earlier versions read
 * too widely and failed against prose that was right. Reading the whole upload section
 * swept in the error siblings, which name {@code image/gif} and {@code image/svg+xml}
 * on purpose as the types that are <em>refused</em>. Narrowing to the section still
 * swept in any other MIME token a writer might reasonably use there — describing the
 * request encoding as {@code multipart/form-data}, or a future metadata part as
 * {@code application/json} — which turned a correct edit into a red build accusing the
 * writer of promising a type the API refuses. Only the sentence beginning
 * {@code Allowed:} is a claim about the allowlist, so only that is read.
 */
class ApiGuideNamesTheRealAllowlistTest {

    private static final Path GUIDE = Path.of("src", "docs", "asciidoc", "api-guide.adoc");

    /** The sentence that states the allowlist. */
    private static final String ALLOWLIST_ANCHOR = "Allowed:";

    /**
     * The section that names types as <em>refused</em>. That is a claim about
     * {@link ContentType#ALLOWED} too, only inverted, and it needs its own assertion:
     * scoping the positive check to one sentence left this one unguarded, so allowing
     * {@code image/gif} and documenting it correctly still left the guide calling it
     * refused four lines further down, with a green build.
     */
    private static final String REFUSED_ANCHOR = "==== Upload Media — Unsupported Type";

    /**
     * Ends that sentence at a full stop <b>followed by whitespace</b>, not at any full
     * stop: a MIME type may carry one ({@code image/vnd.foo}) and cutting there would
     * silently read half the list.
     */
    private static final Pattern SENTENCE_END = Pattern.compile("\\.\\s");

    /**
     * Any quoted MIME type, whatever its family.
     *
     * <p><b>Not a list of families.</b> An earlier version matched
     * {@code image/…|application/pdf}, which fails wrongly the day the allowlist grows
     * a kind it does not know: adding {@code video/mp4} <em>and</em> documenting it
     * correctly still reported the prose as missing a type it named, with a failure
     * message telling the reader to do what they had just done. Video is not
     * hypothetical — {@code UploadMediaUseCase.MAX_BYTES} says it is deferred until a
     * consumer needs it, so the guard would have broken exactly when that landed. A
     * hand-kept vocabulary of what counts is the restatement these tests exist to
     * remove.
     */
    private static final Pattern MIME = Pattern.compile("`([a-z]+/[a-z0-9.+-]+)`");

    /** The size the allowlist sentence promises, as in {@code ≤ 25 MB}. */
    private static final Pattern CAP_MB = Pattern.compile("(\\d+)\\s*MB");

    @Test
    @DisplayName("the guide's upload prose names exactly ContentType.ALLOWED")
    void theGuideNamesEveryAllowedTypeAndNoOther() throws IOException {
        String guide = Files.readString(GUIDE);
        int start = guide.indexOf("==== Upload Media");
        assertThat(start)
                .withFailMessage("No '==== Upload Media' heading in %s — this test is "
                        + "anchored to it and would otherwise pass by reading nothing.", GUIDE)
                .isGreaterThan(0);

        int end = guide.indexOf("\n==== ", start + 1);
        String section = guide.substring(start, end > 0 ? end : guide.length());

        int listStart = section.indexOf(ALLOWLIST_ANCHOR);
        assertThat(listStart)
                .withFailMessage("No '%s' sentence under '==== Upload Media' in %s. This "
                        + "test reads that sentence and nothing else, so a rename would "
                        + "otherwise let it pass by examining no prose at all.",
                        ALLOWLIST_ANCHOR, GUIDE)
                .isGreaterThan(0);
        Matcher sentenceEnd = SENTENCE_END.matcher(section);
        String allowlistSentence = section.substring(
                listStart, sentenceEnd.find(listStart) ? sentenceEnd.start() : section.length());

        List<String> mentioned = MIME.matcher(allowlistSentence).results()
                .map(r -> r.group(1)).distinct().sorted().toList();
        List<String> allowed = ContentType.ALLOWED.stream().sorted().toList();

        assertThat(mentioned)
                .withFailMessage("""
                        The API guide's upload prose and ContentType.ALLOWED disagree.
                          guide names : %s
                          code allows : %s
                        A type the code allows and the guide omits is an undocumented \
                        capability. A type the guide names and the code refuses is a \
                        promise the API breaks — which is how a client comes to upload \
                        an SVG. Update src/docs/asciidoc/api-guide.adoc; the field table \
                        beneath it generates itself.""", mentioned, allowed)
                .isEqualTo(allowed);

        Matcher cap = CAP_MB.matcher(allowlistSentence);
        assertThat(cap.find())
                .withFailMessage("The '%s' sentence states no size cap. It is the only "
                        + "place the guide's prose promises one, so dropping it leaves "
                        + "the limit undocumented.", ALLOWLIST_ANCHOR)
                .isTrue();
        assertThat(Long.parseLong(cap.group(1)))
                .withFailMessage("""
                        The guide's prose and UploadMediaUseCase.MAX_BYTES disagree about \
                        the upload cap: prose says %s MB, code allows %d MB. The field \
                        table beneath generates itself from MAX_BYTES, so raising the cap \
                        leaves the two contradicting each other four words apart.""",
                        cap.group(1), UploadMediaUseCase.MAX_BYTES / (1024 * 1024))
                .isEqualTo(UploadMediaUseCase.MAX_BYTES / (1024 * 1024));

        assertThat(section)
                .withFailMessage("The upload prose states a count of allowed types. "
                        + "Counts go stale the moment the allowlist changes — name the "
                        + "types, which this test keeps true, and drop the number.")
                .doesNotContain("exactly four types");

        assertThat(namedAsRefused(guide))
                .withFailMessage("""
                        The guide names a type as refused that ContentType.ALLOWED now \
                        accepts. The two claims sit four lines apart, so a reader takes \
                        the negative one and does not send a file the API would have \
                        taken. Update the '%s' section, or stop naming the type there.""",
                        REFUSED_ANCHOR)
                .doesNotContainAnyElementsOf(ContentType.ALLOWED);
    }
    /** The types the guide names as refused, from the section that exists to name them. */
    private static List<String> namedAsRefused(String guide) {
        int start = guide.indexOf(REFUSED_ANCHOR);
        assertThat(start)
                .withFailMessage("No '%s' heading in %s. This assertion reads that "
                        + "section and nothing else, so a rename would let it pass by "
                        + "examining no prose at all.", REFUSED_ANCHOR, GUIDE)
                .isGreaterThan(0);
        int end = guide.indexOf("\n==== ", start + 1);
        String section = guide.substring(start, end > 0 ? end : guide.length());
        List<String> refused = MIME.matcher(section).results()
                .map(r -> r.group(1)).distinct().sorted().toList();
        assertThat(refused)
                .withFailMessage("The '%s' section names no type at all. Its whole job is "
                        + "to name the ones that are refused, so an empty result means "
                        + "this assertion is examining nothing rather than finding "
                        + "nothing wrong.", REFUSED_ANCHOR)
                .isNotEmpty();
        return refused;
    }

}
