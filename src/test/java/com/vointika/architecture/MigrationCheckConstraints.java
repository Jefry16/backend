package com.vointika.architecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reads what a column's CHECK constraint currently allows, straight out of the
 * migrations, so a test pinning an enum against one never retypes the list it is
 * checking.
 *
 * <p>It moved here from {@code touroperator}'s test tree when {@code metafield}
 * needed it too, and is now <b>six call sites across three contexts</b>
 * ({@code touroperator} 4, {@code audit} 1, {@code metafield} 1), so it takes the
 * migration folder rather than naming one. The ordering rule below is why this is one
 * helper rather than a copy per context — {@code audit} kept a 119-line copy until
 * 2026-08-19, and the copy had drifted into asserting only one direction.
 */
public final class MigrationCheckConstraints {

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
     *
     * <p><b>Scoped to {@code table}, because a column name is not unique within a
     * context.</b> Each match is attributed to the nearest {@code CREATE TABLE} or
     * {@code ALTER TABLE} above it, and anything on another table is skipped. Without
     * that, {@code experience} already answers wrongly today — {@code status} is a
     * CHECK on {@code experiences} in V1 and on {@code slots} in V4, so last-wins
     * returned the slots values for either question, and the failure message named the
     * table the caller asked about rather than the one it had read. The parameter was
     * accepted and used only in that message.
     */
    static Set<String> allowedValues(Path migrations, String column, String table) throws IOException {
        Pattern check = Pattern.compile("(?<![A-Za-z0-9_])" + column + "\\s+IN\\s*\\(([^)]*)\\)",
                Pattern.CASE_INSENSITIVE);
        List<Path> files = MigrationSql.inVersionOrder(migrations);

        Set<String> latest = new LinkedHashSet<>();
        for (Path file : files) {
            String sql = Files.readString(file, StandardCharsets.UTF_8);
            Matcher matcher = check.matcher(sql);
            List<String> inThisFile = new ArrayList<>();
            while (matcher.find()) {
                if (table.equalsIgnoreCase(MigrationSql.owningTable(sql, matcher.start()))) {
                    inThisFile.add(matcher.group(1));
                }
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
        Set<String> allowed = allowedValues(migrations, column, table);

        // Two causes since the lookup was scoped by table, and the newer one — a table
        // name that matches no CREATE/ALTER TABLE — is the likelier. Naming only the
        // parser sends a reader to debug working code for an argument they mistyped.
        //
        // The suggestion is built from the UNQUALIFIED form, by the same rule the parser
        // uses. Building it from `table` instead made the advice recommend whatever the
        // caller passed: a schema-qualified argument rendered as "pass
        // 'touroperator.x', not 'touroperator.touroperator.x'" — the input echoed back
        // as the fix, for the one mistake this line exists to catch.
        String bare = MigrationSql.unqualified(table);
        assertThat(allowed)
                .withFailMessage("No %s CHECK for table '%s' in %s. Two causes:%n"
                        + "  the table name matches no CREATE/ALTER TABLE in these migrations — it "
                        + "is compared unqualified, so pass '%s', not '%s.%s'%n"
                        + "  or the parser cannot see a constraint that is really there%n"
                        + "Check the argument first. If the table is right, fix the parser — do not "
                        + "delete the test.", column, table, migrations, bare, context, bare)
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

}
