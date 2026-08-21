package com.vointika.experience.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
 * none of them reads a migration, so a later migration that drops and re-adds
 * that FK with {@code CASCADE}, or with no rule at all, would delete operator
 * data on the next category delete with the whole suite green. It was checked
 * once against a live database, which is the right way to establish a fact and
 * the wrong way to keep one.
 *
 * <p>The translation FK is here for the opposite reason: its rows <em>must</em>
 * go with their category, so {@code CASCADE} is equally load-bearing and equally
 * invisible to the unit suite.
 *
 * <p>Mirrors {@code AuditLogEntryColumnWidthsTest}, which parses DDL out of a
 * migration for the same reason — a fact the suite structurally cannot see.
 */
class CategoryForeignKeyRulesTest {

    private static final Path CREATE_CATEGORIES =
            Path.of("src/main/resources/db/migration/experience/V14__create_categories.sql");
    private static final Path ADD_CATEGORY_ID =
            Path.of("src/main/resources/db/migration/experience/V15__experience_category_id.sql");

    /**
     * The FK's {@code ON DELETE} rule, or {@code NO ACTION} where the clause omits
     * one — Postgres's default, and a silent failure of its own here: the delete
     * would raise a foreign-key violation instead of uncategorizing anything.
     */
    private static String deleteRule(String sql, String column) {
        Matcher declaration = Pattern.compile(
                        column + "\\s+UUID\\b[^,;]*?REFERENCES\\s+experience\\.categories\\s*\\(\\s*id\\s*\\)([^,;]*)",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
                .matcher(sql);
        assertThat(declaration.find())
                .withFailMessage("""
                        No `%s UUID … REFERENCES experience.categories (id)` clause found. \
                        If the column or the referenced table was renamed, this test has to \
                        follow it — do not delete the assertion, it is the only thing holding \
                        the delete rule.""", column)
                .isTrue();

        Matcher rule = Pattern.compile(
                        "ON\\s+DELETE\\s+(SET\\s+NULL|CASCADE|RESTRICT|SET\\s+DEFAULT|NO\\s+ACTION)",
                        Pattern.CASE_INSENSITIVE)
                .matcher(declaration.group(1));
        return rule.find()
                ? rule.group(1).replaceAll("\\s+", " ").toUpperCase(Locale.ROOT)
                : "NO ACTION";
    }

    /**
     * Comments stripped before parsing. Both migrations <em>describe</em> their
     * delete rule in prose directly above the DDL that sets it, so a parser that
     * read comments could pass on the description while the statement said the
     * opposite — the exact shape of a guard whose reach does not match its claim.
     */
    private static String ddlOnly(Path migration) throws IOException {
        return Files.readString(migration, StandardCharsets.UTF_8).replaceAll("(?m)--.*$", "");
    }

    @Test
    void deletingACategoryLeavesItsExperiencesBehind() throws IOException {
        assertThat(deleteRule(ddlOnly(ADD_CATEGORY_ID), "category_id"))
                .withFailMessage("""
                        experiences.category_id must be ON DELETE SET NULL. With CASCADE, \
                        deleting one category deletes every experience filed under it; with \
                        NO ACTION the delete fails outright. DeleteCategoryUseCase does not \
                        refuse and does not reassign, so this rule IS the delete semantic.""")
                .isEqualTo("SET NULL");
    }

    /** SET NULL on a NOT NULL column is accepted at DDL time and fails at delete time. */
    @Test
    void theExperienceCategoryColumnStaysNullable() throws IOException {
        assertThat(ddlOnly(ADD_CATEGORY_ID))
                .withFailMessage("""
                        experiences.category_id must stay nullable — uncategorized is a state, \
                        not a missing value, and ON DELETE SET NULL cannot write NULL into a \
                        NOT NULL column.""")
                .doesNotContainIgnoringCase("category_id UUID NOT NULL");
    }

    @Test
    void deletingACategoryTakesItsTranslationsWithIt() throws IOException {
        assertThat(deleteRule(ddlOnly(CREATE_CATEGORIES), "category_id"))
                .withFailMessage("""
                        category_translations.category_id must be ON DELETE CASCADE. An overlay \
                        whose category is gone is unreachable by every read path and would keep \
                        the category id alive as an orphan.""")
                .isEqualTo("CASCADE");
    }

    /**
     * The parser reads DDL and not prose. Without this, both assertions above
     * could be passing on the comment that explains the rule rather than the
     * statement that sets it — and would keep passing after the statement changed.
     */
    @Test
    void theRuleIsReadFromTheStatementAndNotFromACommentAboveIt() {
        String misleading = """
                -- ON DELETE SET NULL is what we want here, obviously.
                ALTER TABLE experience.experiences
                    ADD COLUMN category_id UUID REFERENCES experience.categories (id) ON DELETE CASCADE;
                """;
        String stripped = misleading.replaceAll("(?m)--.*$", "");

        assertThat(deleteRule(stripped, "category_id")).isEqualTo("CASCADE");
    }

    /** A clause with no rule at all reads as Postgres's default rather than as a pass. */
    @Test
    void anOmittedRuleIsReportedRatherThanAssumed() {
        String noRule = """
                ALTER TABLE experience.experiences
                    ADD COLUMN category_id UUID REFERENCES experience.categories (id);
                """;

        assertThat(deleteRule(noRule, "category_id")).isEqualTo("NO ACTION");
    }
}
