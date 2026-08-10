package com.vointika.architecture;

import com.vointika.metafield.domain.valueobject.MetafieldKey;
import com.vointika.metafield.domain.valueobject.MetafieldNamespace;
import com.vointika.metafield.domain.valueobject.MetaobjectType;
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
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The dev seed inserts straight into Postgres, so it can write values the domain
 * would have refused — and nothing in the build runs it.
 *
 * <p><b>Why this exists.</b> It has happened three times. Two audit rows named
 * actions no code emits ({@code contact_message.read},
 * {@code invitation.expired}), and seven metafield keys used underscores while
 * {@link MetafieldKey} accepts only handle shapes. The last one was the worst
 * kind: {@code GET /metaobject-definitions/{id}} answered <b>422</b> in dev on
 * the seeded Boat definition, while the list endpoint served the same rows 200
 * because it projects without constructing the value object. A fixture that the
 * API could never have produced makes a screen look broken and sends whoever
 * finds it into the wrong half of the codebase.
 *
 * <p>So the seed's domain-shaped values are constructed here with the real value
 * objects, and its audit actions are matched against the literals the use cases
 * emit. Regex over SQL is coarse, which is why each extraction carries a
 * minimum-count assertion: a pattern that silently stops matching would weaken
 * this test into passing on nothing.
 */
class DevSeedWritesOnlyValuesTheDomainAcceptsTest {

    private static final Path SEED = Path.of("docker/dev-seed/dev-seed.sql");
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    private static String seed() throws IOException {
        assertThat(Files.exists(SEED))
                .withFailMessage("%s not found. Tests run from the project root; if the seed "
                        + "moved, point this test at it rather than deleting it.", SEED)
                .isTrue();
        return Files.readString(SEED, StandardCharsets.UTF_8);
    }

    private static Set<String> matches(String sql, String regex, int atLeast, String what) {
        Matcher m = Pattern.compile(regex).matcher(sql);
        Set<String> found = new TreeSet<>();
        while (m.find()) {
            found.add(m.group(1));
        }
        assertThat(found.size())
                .withFailMessage("Expected at least %d %s in the seed, found %d. The seed was "
                        + "probably reshaped and this pattern no longer matches — fix the "
                        + "pattern, because a test that finds nothing checks nothing.",
                        atLeast, what, found.size())
                .isGreaterThanOrEqualTo(atLeast);
        return found;
    }

    @Test
    void everySeededMetafieldAndMetaobjectFieldKeyIsOneTheDomainAccepts() throws IOException {
        String sql = seed();
        Set<String> keys = new TreeSet<>();
        keys.addAll(matches(sql, "'(?:EXPERIENCE|PAGE)', '[a-z0-9-]+', '([a-z0-9_-]+)'", 6,
                "metafield definition keys"));
        keys.addAll(matches(sql, ":'mod_boat_id', '([a-z0-9_-]+)',\\s*'[A-Z_]+'", 3,
                "metaobject field keys"));

        for (String key : keys) {
            assertThatCode(() -> new MetafieldKey(key))
                    .withFailMessage("The seed writes the metafield key '%s', which the domain "
                            + "rejects. Reading it back is a 422 — the shape is lowercase "
                            + "letters, digits and hyphens.", key)
                    .doesNotThrowAnyException();
        }
    }

    @Test
    void everySeededNamespaceAndMetaobjectTypeIsOneTheDomainAccepts() throws IOException {
        String sql = seed();

        for (String ns : matches(sql, "'(?:EXPERIENCE|PAGE)', '([a-z0-9_-]+)', '[a-z0-9_-]+'", 1,
                "metafield namespaces")) {
            assertThatCode(() -> new MetafieldNamespace(ns))
                    .withFailMessage("The seed writes the namespace '%s', which the domain rejects.", ns)
                    .doesNotThrowAnyException();
        }
        for (String type : matches(sql, ":'operator_id', '([a-z0-9_-]+)', 'Boat'", 1,
                "metaobject types")) {
            assertThatCode(() -> new MetaobjectType(type))
                    .withFailMessage("The seed writes the metaobject type '%s', which the domain "
                            + "rejects.", type)
                    .doesNotThrowAnyException();
        }
    }

    /**
     * Audit rows are fixture, but they must still be fixture of something real:
     * an action no use case emits describes a state the UI can never receive.
     */
    @Test
    void everySeededAuditActionIsOneSomeUseCaseEmits() throws IOException {
        Set<String> actions = matches(seed(), "'([a-z_]+\\.[a-z_]+)',", 15, "audit actions");
        String sources = allMainSources();

        Set<String> orphans = new TreeSet<>();
        for (String action : actions) {
            if (!sources.contains('"' + action + '"')) {
                orphans.add(action);
            }
        }

        assertThat(orphans)
                .withFailMessage("The seed writes audit actions that no use case emits:%n%s%n"
                        + "Either the action was renamed and the seed did not follow, or the row "
                        + "is inventing history the application cannot produce.",
                        orphans.stream().map(a -> "  - " + a).reduce((a, b) -> a + "\n" + b).orElse(""))
                .isEmpty();
    }

    private static String allMainSources() throws IOException {
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            List<Path> java = files.filter(f -> f.toString().endsWith(".java")).toList();
            StringBuilder all = new StringBuilder();
            for (Path f : java) {
                all.append(Files.readString(f, StandardCharsets.UTF_8));
            }
            return all.toString();
        }
    }
}
