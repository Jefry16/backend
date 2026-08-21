package com.vointika.experience.infrastructure.persistence;

import com.vointika.architecture.MigrationForeignKeys;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The two category foreign keys carry the delete rules the feature depends on,
 * read out of the migrations themselves.
 *
 * <p><b>Why this is a test and not a verification.</b> {@code DeleteCategoryUseCase}
 * deliberately neither refuses nor reassigns — deleting a category is supposed to
 * leave its experiences behind, uncategorized — so "the experiences survive" is
 * true <em>only</em> because {@code experiences.category_id} is
 * {@code ON DELETE SET NULL}. Every test in the slice mocks the repository and
 * none of them reads a migration, so a change to that rule would destroy operator
 * data on the next category delete with the whole suite green. It was checked once
 * against a live database, which is the right way to establish a fact and the
 * wrong way to keep one.
 *
 * <p><b>It reads the whole migration folder, not V14 and V15.</b> Naming those two
 * files was the first version of this test and it watched the wrong path: editing
 * an applied migration is already forbidden, so the way this rule realistically
 * changes is a <em>later</em> migration dropping and re-adding the constraint —
 * the shape {@code experience/V9} and four others in this repository already use.
 * A V16 doing exactly that left all the assertions passing. {@link MigrationForeignKeys}
 * scans in version order and keeps the last statement to touch the key.
 *
 * <p>The translation FK is here for the opposite reason: its rows <em>must</em> go
 * with their category, so {@code CASCADE} is equally load-bearing and equally
 * invisible to the unit suite.
 */
class CategoryForeignKeyRulesTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration/experience");

    /**
     * Scoped to the table, because {@code category_id} names a column on both of
     * them and the two want opposite rules — the scan cannot tell them apart by
     * column name alone.
     */
    private static String ruleOn(String table) throws IOException {
        return MigrationForeignKeys.deleteRule(MIGRATIONS, "category_id", table);
    }

    @Test
    void deletingACategoryLeavesItsExperiencesBehind() throws IOException {
        assertThat(ruleOn("experiences"))
                .withFailMessage("""
                        experiences.category_id must be ON DELETE SET NULL, and the latest \
                        migration to touch it does not say so. With CASCADE, deleting one \
                        category deletes every experience filed under it; with NO ACTION the \
                        delete fails outright; with NONE the key is gone. DeleteCategoryUseCase \
                        does not refuse and does not reassign, so this rule IS the delete \
                        semantic.""")
                .isEqualTo("SET NULL");
    }

    @Test
    void deletingACategoryTakesItsTranslationsWithIt() throws IOException {
        assertThat(ruleOn("category_translations"))
                .withFailMessage("""
                        category_translations.category_id must be ON DELETE CASCADE. An overlay \
                        whose category is gone is unreachable by every read path and would keep \
                        the category id alive as an orphan.""")
                .isEqualTo("CASCADE");
    }

    /**
     * {@code SET NULL} against a {@code NOT NULL} column is accepted at DDL time and
     * fails at delete time, so the two facts break the same feature by different
     * routes and belong together.
     */
    @Test
    void theExperienceCategoryColumnStaysNullable() throws IOException {
        String declaration = Files.readString(
                MIGRATIONS.resolve("V15__experience_category_id.sql"), StandardCharsets.UTF_8);

        assertThat(declaration.replaceAll("(?m)--.*$", ""))
                .withFailMessage("""
                        experiences.category_id must stay nullable — uncategorized is a state, \
                        not a missing value, and ON DELETE SET NULL cannot write NULL into a \
                        NOT NULL column.""")
                .doesNotContainIgnoringCase("category_id UUID NOT NULL");
    }
}
