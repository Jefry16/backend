package com.vointika.architecture;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The parser's own reach, pinned — because every guard built on it inherits
 * whatever it cannot see, and a guard that silently reads the wrong thing passes
 * loudly.
 *
 * <p>Each case here is a way the first version of {@code CategoryForeignKeyRulesTest}
 * was wrong or could have been: it named two files, so a later migration was
 * invisible; and both of those files quote their own delete rule in prose, so a
 * parser reading comments would have passed on the description.
 */
class MigrationForeignKeysTest {

    @TempDir
    Path migrations;

    private void migration(String name, String sql) throws IOException {
        Files.writeString(migrations.resolve(name), sql);
    }

    private String rule(String table) throws IOException {
        return MigrationForeignKeys.deleteRule(migrations, "category_id", table);
    }

    /** The finding this parser exists for: the sanctioned way to change a constraint. */
    @Test
    void aLaterMigrationThatRePointsTheKeyWins() throws IOException {
        migration("V15__add.sql", """
                ALTER TABLE experience.experiences
                    ADD COLUMN category_id UUID REFERENCES experience.categories (id) ON DELETE SET NULL;
                """);
        migration("V16__repoint.sql", """
                ALTER TABLE experience.experiences DROP CONSTRAINT experiences_category_id_fkey;
                ALTER TABLE experience.experiences
                    ADD CONSTRAINT experiences_category_id_fkey
                        FOREIGN KEY (category_id) REFERENCES experience.categories (id) ON DELETE CASCADE;
                """);

        assertThat(rule("experiences")).isEqualTo("CASCADE");
    }

    /** Ordered on the parsed version: lexicographically V9 sorts after V16. */
    @Test
    void versionOrderIsNumericNotLexical() throws IOException {
        migration("V9__early.sql", """
                ALTER TABLE experience.experiences
                    ADD COLUMN category_id UUID REFERENCES experience.categories (id) ON DELETE CASCADE;
                """);
        migration("V16__later.sql", """
                ALTER TABLE experience.experiences DROP CONSTRAINT experiences_category_id_fkey;
                ALTER TABLE experience.experiences
                    ADD CONSTRAINT experiences_category_id_fkey
                        FOREIGN KEY (category_id) REFERENCES experience.categories (id) ON DELETE SET NULL;
                """);

        assertThat(rule("experiences")).isEqualTo("SET NULL");
    }

    @Test
    void aDropWithNoReAddLeavesNoKeyRatherThanTheStaleRule() throws IOException {
        migration("V15__add.sql", """
                ALTER TABLE experience.experiences
                    ADD COLUMN category_id UUID REFERENCES experience.categories (id) ON DELETE SET NULL;
                """);
        migration("V16__drop.sql", """
                ALTER TABLE experience.experiences DROP CONSTRAINT experiences_category_id_fkey;
                """);

        assertThat(rule("experiences")).isEqualTo(MigrationForeignKeys.NONE);
    }

    /** Within one file the last statement wins, not the first. */
    @Test
    void aDropAndReAddInOneFileEndsOnTheReAdd() throws IOException {
        migration("V16__both.sql", """
                ALTER TABLE experience.experiences DROP CONSTRAINT experiences_category_id_fkey;
                ALTER TABLE experience.experiences
                    ADD CONSTRAINT experiences_category_id_fkey
                        FOREIGN KEY (category_id) REFERENCES experience.categories (id) ON DELETE RESTRICT;
                """);

        assertThat(rule("experiences")).isEqualTo("RESTRICT");
    }

    @Test
    void theRuleIsReadFromTheStatementAndNotFromACommentAboveIt() throws IOException {
        migration("V15__misleading.sql", """
                -- ON DELETE SET NULL is the decided delete semantic here.
                ALTER TABLE experience.experiences
                    ADD COLUMN category_id UUID REFERENCES experience.categories (id) ON DELETE CASCADE;
                """);

        assertThat(rule("experiences")).isEqualTo("CASCADE");
    }

    @Test
    void anOmittedRuleIsPostgresDefaultRatherThanAPass() throws IOException {
        migration("V15__bare.sql", """
                ALTER TABLE experience.experiences
                    ADD COLUMN category_id UUID REFERENCES experience.categories (id);
                """);

        assertThat(rule("experiences")).isEqualTo(MigrationForeignKeys.NO_ACTION);
    }

    /**
     * Both real tables declare a {@code category_id}, and they want opposite rules.
     * Without table scoping whichever appeared last would answer both questions.
     */
    @Test
    void twoTablesWithTheSameColumnAreKeptApart() throws IOException {
        migration("V14__create.sql", """
                CREATE TABLE experience.category_translations (
                    category_id UUID NOT NULL REFERENCES experience.categories (id) ON DELETE CASCADE,
                    locale      VARCHAR(8) NOT NULL,
                    PRIMARY KEY (category_id, locale)
                );
                """);
        migration("V15__add.sql", """
                ALTER TABLE experience.experiences
                    ADD COLUMN category_id UUID REFERENCES experience.categories (id) ON DELETE SET NULL;
                """);

        assertThat(rule("category_translations")).isEqualTo("CASCADE");
        assertThat(rule("experiences")).isEqualTo("SET NULL");
    }

    /** A column no migration references answers NONE rather than throwing. */
    @Test
    void aKeyNoMigrationDeclaresIsNone() throws IOException {
        migration("V1__unrelated.sql", """
                CREATE TABLE experience.experiences (id UUID NOT NULL PRIMARY KEY);
                """);

        assertThat(rule("experiences")).isEqualTo(MigrationForeignKeys.NONE);
    }

    /**
     * The second blind spot, and the one with a live instance:
     * {@code touroperator/V13} drops a foreign key whose name it looks up in
     * {@code pg_constraint} and executes through {@code EXECUTE format(…)}. There is
     * no constraint name in the file to match, so no static parser sees it — this
     * pins that as known reach rather than leaving the next caller to find it.
     *
     * <p>It is why the javadoc on {@code deleteRule} calls the name convention a
     * default rather than a checked one: the repository's only FK drop is this shape.
     *
     * <p><b>Two mechanisms hide this drop, so no single mutation makes this fail —
     * measured, not assumed.</b> {@code %I} is not an identifier token, so the drop
     * pattern does not match it at all; and even when widened to accept one, the
     * placeholder name does not contain the column, so the name filter skips it.
     * Each mutation alone leaves all ten green; applying both fails this test and
     * {@code aDropUnderAnUnrelatedNameIsNotSeen} together. So read this as a
     * <em>documentation</em> test that records a shape, not as a guard that bites on
     * the next edit — the sibling below is the one with single-mutation reach.
     */
    @Test
    void aDropBuiltAtRuntimeIsNotSeen() throws IOException {
        migration("V15__add.sql", """
                ALTER TABLE experience.experiences
                    ADD COLUMN category_id UUID REFERENCES experience.categories (id) ON DELETE SET NULL;
                """);
        migration("V16__dynamic.sql", """
                DO $$
                DECLARE fk_name text;
                BEGIN
                    SELECT conname INTO fk_name FROM pg_constraint
                    WHERE conrelid = 'experience.experiences'::regclass AND contype = 'f';
                    EXECUTE format('ALTER TABLE experience.experiences DROP CONSTRAINT %I', fk_name);
                END $$;
                """);

        assertThat(rule("experiences")).isEqualTo("SET NULL");
    }

    /**
     * The first blind spot, stated rather than discovered later: a drop under a name
     * that does not mention the column is not attributed to it, so the previous rule
     * stands. Widening to "any DROP CONSTRAINT on this table" would fail on
     * {@code experience/V9}, which drops a CHECK.
     */
    @Test
    void aDropUnderAnUnrelatedNameIsNotSeen() throws IOException {
        migration("V15__add.sql", """
                ALTER TABLE experience.experiences
                    ADD COLUMN category_id UUID REFERENCES experience.categories (id) ON DELETE SET NULL;
                """);
        migration("V16__hand_named.sql", """
                ALTER TABLE experience.experiences DROP CONSTRAINT fk_taxonomy_link;
                """);

        assertThat(rule("experiences")).isEqualTo("SET NULL");
    }
}
