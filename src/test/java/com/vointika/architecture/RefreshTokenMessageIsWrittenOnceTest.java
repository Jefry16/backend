package com.vointika.architecture;

import com.vointika.identity.domain.entity.RefreshToken;
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
 * The refresh-token 401 exists once, as a constant, and nowhere as a literal.
 *
 * <p><b>What this protects is not tidiness.</b> Five throws answer with it, four of them
 * in {@code RefreshAccessTokenUseCase}: an unknown hash, a <b>revoked token being
 * replayed</b>, a token whose user is gone, and the loser of a rotation race. If the
 * replay branch's message ever drifted, the response <em>body</em> would tell an attacker
 * that the token they stole was recognised.
 *
 * <p><b>Scoped to the body deliberately, because the branch is not otherwise silent.</b>
 * Replay logs a warning and runs {@code revokeAllByFamilyId} — an UPDATE — before
 * throwing, while the unknown-hash branch throws straight off a failed lookup with no
 * write and no log. So it answers measurably slower, and repeatably: replaying the same
 * revoked token re-enters the branch every time, so the difference can be averaged over
 * as many requests as an attacker likes. <b>This guard makes the body indistinguishable;
 * it cannot make the endpoint indistinguishable</b>, and no string ever could.
 *
 * <p>That is judged acceptable rather than overlooked: reaching the branch at all
 * requires holding a genuine token — 32 bytes of {@code SecureRandom} — whose family is
 * already dead by the time the timing is measurable. Padding the unknown path with a
 * dummy write would be worse code bought for very little.
 *
 * <p><b>And unlike {@code TENANT_NOT_FOUND}, no structure holds it.</b> That one throws
 * once behind a single predicate, so its causes cannot differ whatever any string says —
 * `PATTERNS.md` §9a's "write the guard for the copies and credit the structure for the
 * property". Here there is no structure to credit: five separate {@code throw}
 * statements, and identical literals were the whole of it. So this guard is load-bearing
 * in a way that one is not.
 *
 * <p><b>What it does NOT catch</b>, as ever: one site <em>diverging</em> to a near-miss
 * sentence passes, because the literal it looks for is gone. Each of the five branches
 * has a test asserting the constant, which is what covers that — including the
 * user-is-gone branch, which had none until this change.
 *
 * <p><b>Two exemptions, and exactly two.</b> The declaration, and one pinning assertion
 * on the reuse branch. Without the second the sentence would be spelled nowhere: every
 * other assertion now reads the constant, so they hold for any value, and a reword would
 * move the published {@code auth/refresh-invalid} body with a green suite — strictly
 * weaker than the copies this replaced (#184, #189).
 */
class RefreshTokenMessageIsWrittenOnceTest {

    private static final Path DECLARATION =
            Path.of("src", "main", "java", "com", "vointika", "identity", "domain", "entity",
                    "RefreshToken.java");

    /** The one assertion allowed to spell it — on the reuse branch, deliberately. */
    private static final Path PIN =
            Path.of("src", "test", "java", "com", "vointika", "identity", "application", "usecase",
                    "RefreshAccessTokenUseCaseTest.java");

    private static final String LITERAL = '"' + RefreshToken.INVALID + '"';

    @Test
    @DisplayName("the refresh-token 401 is a constant, never a literal")
    void theMessageIsWrittenOnce() throws IOException {
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        for (Path root : List.of(Path.of("src", "main", "java"), Path.of("src", "test", "java"))) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    scanned++;
                    if (file.equals(DECLARATION) || file.equals(PIN)) {
                        continue;
                    }
                    if (Files.readString(file).contains(LITERAL)) {
                        offenders.add(file.toString());
                    }
                }
            }
        }

        assertThat(scanned)
                .withFailMessage("Scanned %d java files — the walk found nothing, so this "
                        + "assertion proves nothing. Check the roots.", scanned)
                .isGreaterThan(500);

        assertThat(offenders)
                .withFailMessage("""
                        %s is written as a literal in %d place(s):
                        %s
                        Five throws answer with that sentence and they must stay byte-identical: \
                        an unknown token, a REPLAYED one (which also revokes the family), a \
                        token whose user is gone, a lost rotation race, and logout with an unknown \
                        token. A copy is how they drift apart, and the branch that must not stand \
                        out is the replay — a distinguishable answer there tells an attacker their \
                        stolen token was recognised.
                        Use RefreshToken.INVALID.""",
                        LITERAL, offenders.size(), String.join("\n", offenders))
                .isEmpty();
    }

    /** The pin only pins while it is the sole exemption; a second one makes it meaningless. */
    @Test
    void exactlyOneAssertionSpellsTheSentence() throws IOException {
        long inPin = Files.readString(PIN).split(java.util.regex.Pattern.quote(LITERAL), -1).length - 1;

        assertThat(inPin)
                .withFailMessage("The exempted file spells %s %d time(s); it must be exactly once. "
                        + "More than one and 'written once' stops meaning what the other assertion "
                        + "promises; none and the wording is pinned nowhere at all.", LITERAL, inPin)
                .isEqualTo(1);
    }
}
