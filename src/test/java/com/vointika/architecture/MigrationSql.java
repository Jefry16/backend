package com.vointika.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The mechanics every migration-reading guard needs: version order, which table a
 * statement belongs to, and comment stripping.
 *
 * <p>Extracted at the second caller, not before (LAW §2.4).
 * {@link MigrationCheckConstraints} held these privately while it was the only
 * one; {@link MigrationForeignKeys} needs the identical rules, and a second copy
 * of "order by parsed version" is exactly the drift `audit`'s 119-line duplicate
 * of that class already demonstrated.
 */
final class MigrationSql {

    private static final Pattern VERSION = Pattern.compile("^V(\\d+)__");

    private static final Pattern OWNER = Pattern.compile(
            "(?:CREATE|ALTER)\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?([A-Za-z0-9_.\"]+)",
            Pattern.CASE_INSENSITIVE);

    private MigrationSql() {
    }

    /**
     * A context's migrations, oldest first.
     *
     * <p>Ordered on the parsed version, not the filename: lexicographically
     * {@code V10} sorts before {@code V1}, which would silently apply the wrong
     * one last — and {@code experience} is past V10, so that is live rather than
     * hypothetical.
     */
    static List<Path> inVersionOrder(Path migrations) throws IOException {
        try (Stream<Path> paths = Files.list(migrations)) {
            return paths.filter(p -> p.toString().endsWith(".sql"))
                    .sorted(Comparator.comparingInt(MigrationSql::version))
                    .toList();
        }
    }

    /**
     * The table of the nearest {@code CREATE}/{@code ALTER TABLE} above
     * {@code position}, unqualified — every migration here writes
     * {@code CREATE TABLE audit.audit_log}, and callers name the bare table.
     *
     * <p><b>Nearest-statement-above is textual, so a comment counts.</b> Pass
     * {@link #withoutComments} output where a migration's prose might name a table
     * it is not altering; the check-constraint caller does not, and has been
     * checked across every migration for it.
     */
    static String owningTable(String sql, int position) {
        Matcher matcher = OWNER.matcher(sql);
        String current = null;
        while (matcher.find() && matcher.start() < position) {
            current = unqualified(matcher.group(1).replace("\"", ""));
        }
        return current;
    }

    /** {@code audit.audit_log} → {@code audit_log}. */
    static String unqualified(String name) {
        return name.substring(name.lastIndexOf('.') + 1);
    }

    /**
     * Line comments removed.
     *
     * <p>Migrations here explain a constraint in prose directly above the statement
     * that sets it, often quoting the clause verbatim, so a guard that reads the raw
     * file can match the explanation and keep passing after the statement changed
     * underneath it.
     */
    static String withoutComments(String sql) {
        return sql.replaceAll("(?m)--.*$", "");
    }

    static int version(Path migration) {
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
