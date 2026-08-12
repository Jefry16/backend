package com.vointika.architecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads what a column's CHECK constraint currently allows, straight out of the
 * migrations, so a test pinning an enum against one never retypes the list it is
 * checking.
 *
 * <p>It moved here from {@code touroperator}'s test tree when {@code metafield}
 * needed it too — three callers now, in two contexts, so it takes the migration
 * folder rather than naming one. The ordering rule below is why this is one
 * helper rather than a copy per context.
 */
public final class MigrationCheckConstraints {

    private static final Pattern VERSION = Pattern.compile("^V(\\d+)__");

    private MigrationCheckConstraints() {
    }

    /**
     * The values the latest migration to mention {@code column} allows.
     *
     * <p>Reads the migrations in version order and keeps the <em>last</em> check
     * it finds, so a later migration that replaces the constraint wins over the
     * one that created it. Union-ing them would let a narrowing migration pass.
     *
     * <p>Ordered on the parsed version, not the filename: lexicographically
     * {@code V10} sorts before {@code V1}, which would silently keep the wrong
     * constraint — and this context is already past V10, so that is live rather
     * than hypothetical.
     *
     * <p>The column name is matched on a boundary, which {@code type} is what
     * forced: an unanchored match reads {@code link_type IN (…)} from the menus
     * migration as this column's constraint.
     */
    static Set<String> allowedValues(Path migrations, String column) throws IOException {
        Pattern check = Pattern.compile("(?<![A-Za-z0-9_])" + column + "\\s+IN\\s*\\(([^)]*)\\)",
                Pattern.CASE_INSENSITIVE);
        List<Path> files;
        try (Stream<Path> paths = Files.list(migrations)) {
            files = paths.filter(p -> p.toString().endsWith(".sql"))
                    .sorted(Comparator.comparingInt(MigrationCheckConstraints::version))
                    .toList();
        }

        Set<String> latest = new LinkedHashSet<>();
        for (Path file : files) {
            Matcher matcher = check.matcher(Files.readString(file, StandardCharsets.UTF_8));
            List<String> inThisFile = new ArrayList<>();
            while (matcher.find()) {
                inThisFile.add(matcher.group(1));
            }
            if (!inThisFile.isEmpty()) {
                latest.clear();
                for (String value : inThisFile.getLast().split(",")) {
                    latest.add(value.trim().replace("'", "").replace("\n", ""));
                }
            }
        }
        return latest;
    }

    /**
     * <b>The two lists must match exactly, not one contain the other</b>, because
     * every column pinned this way is mapped {@code @Enumerated(STRING)} and the
     * coupling therefore runs both ways: a value only the enum has fails every
     * insert with SQLSTATE 23514, which nothing translates (only 23505 is), and a
     * value only the CHECK has fails the <em>read</em>, on a public page.
     */
    public static void assertEnumMatches(String context, String column,
                                        Class<? extends Enum<?>> type, String table) throws IOException {
        Path migrations = Path.of("src/main/resources/db/migration", context);
        Set<String> allowed = allowedValues(migrations, column);

        assertThat(allowed)
                .withFailMessage("No %s CHECK found in %s — this test cannot see the constraint it "
                        + "exists to track. Fix the parser, do not delete the test.", column, migrations)
                .isNotEmpty();

        Set<String> declared = Arrays.stream(type.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(allowed)
                .withFailMessage(
                        "%s declares %s but the %s.%s constraint allows %s, and the two must match "
                                + "exactly — the coupling runs both ways:%n"
                                + "  in the enum, not in the CHECK -> every insert of that value fails "
                                + "with SQLSTATE 23514, which nothing translates (only 23505 is)%n"
                                + "  in the CHECK, not in the enum -> the column is mapped "
                                + "@Enumerated(STRING), so a row carrying it fails the READ, on a public "
                                + "storefront page%n"
                                + "Ship the migration and the enum change together.",
                        type.getSimpleName(), declared, table, column, allowed)
                .containsExactlyInAnyOrderElementsOf(declared);
    }

    private static int version(Path migration) {
        String name = migration.getFileName().toString();
        Matcher matcher = VERSION.matcher(name);
        if (!matcher.find()) {
            throw new IllegalStateException(
                    "Migration '" + name + "' is not V<int>__name.sql, so this test cannot order it "
                            + "against the others. Teach the parser the new scheme — silently "
                            + "mis-ordering would let a narrowing constraint through.");
        }
        return Integer.parseInt(matcher.group(1));
    }
}
