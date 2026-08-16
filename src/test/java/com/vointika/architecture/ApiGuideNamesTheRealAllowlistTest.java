package com.vointika.architecture;

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
 */
class ApiGuideNamesTheRealAllowlistTest {

    private static final Path GUIDE = Path.of("src", "docs", "asciidoc", "api-guide.adoc");

    /** Any MIME type the prose mentions, in the section that describes uploads. */
    private static final Pattern MIME = Pattern.compile("`(image/[a-z0-9.+-]+|application/pdf)`");

    @Test
    @DisplayName("the guide's upload prose names exactly ContentType.ALLOWED")
    void theGuideNamesEveryAllowedTypeAndNoOther() throws IOException {
        String guide = Files.readString(GUIDE);
        int start = guide.indexOf("==== Upload Media");
        assertThat(start)
                .withFailMessage("No '==== Upload Media' heading in %s — this test is "
                        + "anchored to it and would otherwise pass by reading nothing.", GUIDE)
                .isGreaterThan(0);

        // The upload section ONLY, stopping at its first error sibling. The sibling
        // sections name types on purpose — "image/gif and image/svg+xml are both
        // refused" — and reading those as claims about the allowlist is how this test
        // failed on prose that was correct.
        int end = guide.indexOf("\n==== ", start + 1);
        String section = guide.substring(start, end > 0 ? end : guide.length());

        List<String> mentioned = MIME.matcher(section).results()
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

        assertThat(section)
                .withFailMessage("The upload prose states a count of allowed types. "
                        + "Counts go stale the moment the allowlist changes — name the "
                        + "types, which this test keeps true, and drop the number.")
                .doesNotContain("exactly four types");
    }
}
