package com.vointika.architecture;

import com.vointika.shared.port.TourOperatorMembershipCheck;
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
 * The tenant-isolation 404 exists once, as a constant, and nowhere as a literal.
 *
 * <p><b>What this enforces: the sentence exists once, so it can be changed once.</b>
 * It was written out <b>twenty times across nineteen files</b> in {@code src/main}
 * and sixteen more in tests. This fails the build if it reappears as a literal
 * anywhere but the constant's own declaration.
 *
 * <p><b>What this does NOT enforce, and what does.</b> It catches the sentence being
 * <em>copied</em>, not a site <em>diverging</em>: change one throw to "Tour operator
 * was not found" and this passes, because the literal it looks for is gone. Caught in
 * review by exactly that mutation.
 *
 * <p>The property that actually protects tenant isolation is structural, not this
 * test. {@code TourOperatorMembershipPolicy.ensureMember} throws <b>once</b>, behind a
 * single predicate that is false both when the operator does not exist and when the
 * caller is not a member — so the two enumeration-relevant causes cannot answer
 * differently, whatever any string says. The interceptor's malformed-id and
 * no-principal branches are the ones that could drift, and a caller can already tell a
 * malformed UUID apart without asking the server.
 *
 * <p>So: keep this guard for what it is worth — twenty copies do not come back — and
 * do not read it as proof that a <em>new</em> throw site is safe.
 *
 * <p><b>Scope is both trees deliberately.</b> A test that hardcodes it is the same
 * defect one step removed: it keeps passing after the constant is reworded, so the
 * suite would report agreement that no longer exists.
 */
class TenantNotFoundMessageIsWrittenOnceTest {

    private static final Path DECLARATION =
            Path.of("src", "main", "java", "com", "vointika", "shared", "port",
                    "TourOperatorMembershipCheck.java");

    private static final String LITERAL = '"' + TourOperatorMembershipCheck.TENANT_NOT_FOUND + '"';

    @Test
    @DisplayName("the tenant 404 is a constant, never a literal")
    void theMessageIsWrittenOnce() throws IOException {
        List<String> offenders = new ArrayList<>();
        int scanned = 0;

        for (Path root : List.of(Path.of("src", "main", "java"), Path.of("src", "test", "java"))) {
            try (Stream<Path> files = Files.walk(root)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    scanned++;
                    if (file.equals(DECLARATION)) {
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
                        That sentence is the tenant-isolation answer for four different \
                        causes — a missing operator, a non-member, a malformed id, and an \
                        unauthenticated caller — and they must stay byte-identical or a \
                        caller can tell them apart and enumerate operators. Use \
                        TourOperatorMembershipCheck.TENANT_NOT_FOUND, or \
                        TourOperatorRepository.requireById for a lookup.""",
                        LITERAL, offenders.size(), String.join("\n", offenders))
                .isEmpty();
    }
}
