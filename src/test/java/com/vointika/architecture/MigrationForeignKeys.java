package com.vointika.architecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What a foreign key's {@code ON DELETE} rule currently is, read out of a
 * context's migrations.
 *
 * <p><b>It reads the whole folder, not the migration that created the key.</b>
 * `CLAUDE.md` forbids editing an applied migration, so the sanctioned way to
 * change a constraint is a later one that drops and re-adds it — a shape five
 * migrations across four contexts already use, {@code experiences} among them
 * ({@code experience/V9}). A guard pointed at the creating file watches the
 * change that is prohibited and is blind to the change that is prescribed.
 *
 * <p>Same ordering rule as {@link MigrationCheckConstraints}: version order,
 * last-wins. Union-ing would let a later weakening pass.
 */
public final class MigrationForeignKeys {

    /** No foreign key on that column at all — dropped and never re-added, or never created. */
    public static final String NONE = "NONE";

    /** Postgres's default when a {@code REFERENCES} clause names no rule. */
    public static final String NO_ACTION = "NO ACTION";

    private static final Pattern RULE = Pattern.compile(
            "ON\\s+DELETE\\s+(SET\\s+NULL|CASCADE|RESTRICT|SET\\s+DEFAULT|NO\\s+ACTION)",
            Pattern.CASE_INSENSITIVE);

    private MigrationForeignKeys() {
    }

    /**
     * The rule the latest migration to touch this {@code (table, column)} foreign
     * key leaves in place.
     *
     * <p>Three statement shapes set one, and all three are read: an inline
     * {@code REFERENCES} in a {@code CREATE TABLE}, an {@code ADD COLUMN … REFERENCES},
     * and an {@code ADD CONSTRAINT … FOREIGN KEY (col) REFERENCES}. A
     * {@code DROP CONSTRAINT} naming the column removes it again, so a drop with no
     * re-add answers {@link #NONE} rather than the stale rule.
     *
     * <p><b>The drop is matched on the constraint name containing the column</b>,
     * which is Postgres's own {@code <table>_<column>_fkey} convention and what every
     * drop in this repository uses. A drop under a hand-chosen name that does not
     * mention the column is not seen, and the previous rule would stand — the one
     * reach this parser does not have. Widening it to "any DROP CONSTRAINT on this
     * table" was the alternative and is worse: {@code experience/V9} drops a CHECK on
     * {@code experiences}, so every caller would fail on a migration that never touched
     * a foreign key.
     */
    public static String deleteRule(Path migrations, String column, String table) throws IOException {
        Pattern reference = Pattern.compile(
                "(?<![A-Za-z0-9_])" + column + "\\s*\\)?\\s+(?:UUID\\s+)?[^,;]*?REFERENCES\\s+[A-Za-z0-9_.\"]+\\s*\\([^)]*\\)([^,;]*)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Pattern drop = Pattern.compile(
                "DROP\\s+CONSTRAINT\\s+(?:IF\\s+EXISTS\\s+)?([A-Za-z0-9_.\"]+)",
                Pattern.CASE_INSENSITIVE);

        String current = NONE;
        for (Path file : MigrationSql.inVersionOrder(migrations)) {
            String sql = MigrationSql.withoutComments(
                    Files.readString(file, StandardCharsets.UTF_8));

            // Keyed on position so a drop-then-add inside one file ends on the add,
            // and an add-then-drop ends on the drop.
            NavigableMap<Integer, String> events = new TreeMap<>();

            Matcher added = reference.matcher(sql);
            while (added.find()) {
                if (!table.equalsIgnoreCase(MigrationSql.owningTable(sql, added.start()))) {
                    continue;
                }
                Matcher rule = RULE.matcher(added.group(1));
                events.put(added.start(), rule.find()
                        ? rule.group(1).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT)
                        : NO_ACTION);
            }

            Matcher dropped = drop.matcher(sql);
            while (dropped.find()) {
                String name = MigrationSql.unqualified(dropped.group(1).replace("\"", ""));
                if (!name.toLowerCase(Locale.ROOT).contains(column.toLowerCase(Locale.ROOT))
                        || !table.equalsIgnoreCase(MigrationSql.owningTable(sql, dropped.start()))) {
                    continue;
                }
                events.put(dropped.start(), NONE);
            }

            if (!events.isEmpty()) {
                current = events.lastEntry().getValue();
            }
        }
        return current;
    }
}
