package com.vointika.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every field a list can be sorted by maps to a column that is never null.
 *
 * <p><b>Why this rule exists.</b> Lists page with a <b>keyset cursor</b>, not
 * {@code OFFSET}: {@code CriteriaListExecutor} remembers the last row's
 * {@code (sortValue, id)} and the next page asks for everything after it —
 * {@code greaterThan}/{@code lessThan} plus an {@code equal} tie-break on
 * {@code id}. That is fast and correct, and it assumes the sort value exists.
 *
 * <p>Give it a nullable column and two things break, one loudly and one not:
 * <ul>
 *   <li>the cursor is built with {@code String.valueOf(...)}, so a null becomes
 *       the literal {@code "null"} and the next request 422s on a cursor the
 *       server itself issued;
 *   <li><b>and the quiet one</b> — in SQL a {@code NULL} comparison is
 *       <em>unknown</em>, and {@code WHERE} keeps only rows that are true. A null
 *       row satisfies neither the comparison nor the {@code equal} tie-break, so
 *       <b>it can never appear after page one</b>. 100 rows with 30 nulls returns
 *       20, then silently omits those 30 forever. No error, no log line — the list
 *       is just short, and a report built from it is wrong and looks normal.
 * </ul>
 *
 * <p><b>Nothing violates this today</b>, which is exactly why it is worth pinning:
 * the invariant currently holds by accident. The realistic failure is someone
 * adding {@code .sortable("nickname")} on a nullable column — it passes every
 * test, because tests rarely page past the first page; it works in dev, because
 * the seed fills the column; and it breaks in production only for the customers
 * with enough data to paginate.
 *
 * <p><b>Sorting is forbidden outright; filtering is not.</b> A nullable column
 * filters correctly with a positive operator — a null genuinely does not contain
 * "x" — so {@link FilterableNullableColumnsAreDeclaredTest} makes it a
 * declaration and refuses only the negating operators. Here there is nothing to
 * declare: no sort over a nullable column is correct.
 *
 * <p>The source scanning lives in {@link ListSchemaScanner}, shared with that
 * guard so a blind spot fixed in one cannot leave the other blind.
 *
 * <p><b>Known limit, stated rather than implied.</b> This reads what the
 * application declares ({@code @Column(nullable = false)}), not what the database
 * enforces. An entity that disagrees with its migration is a real bug, and a
 * different one from this.
 */
class SortableColumnsAreNeverNullableTest {

    @Test
    void everySortableFieldMapsToANonNullableColumn() throws IOException {
        ListSchemaScanner.Scan scan = ListSchemaScanner.scan();
        Map<String, String> entityByUseCase = scan.entityByUseCase();

        // Reported in both directions. Subtracting the sizes was tempting and wrong:
        // a use case paired but with no parsed schema makes the count negative and
        // the list empty, so a real failure would print "0 class(es):" and nothing.
        assertThat(entityByUseCase.keySet())
                .withFailMessage("""
                        The ListSchema declarations and the entity pairings disagree, so some \
                        endpoint's sortable columns went unchecked.
                        Declares a ListSchema, never paired with an entity: %s
                        Paired with an entity, no ListSchema this test could parse: %s
                        The pairing is read from `listExecutor.list(FooJpaEntity.class, \
                        SomeUseCase.SCHEMA, …)`. Either a list is wired some other way — teach the \
                        scanner how to find it — or the scan broke.""",
                        ListSchemaScanner.minus(scan.declaresASchema(), entityByUseCase.keySet()),
                        ListSchemaScanner.minus(entityByUseCase.keySet(), scan.declaresASchema()))
                .containsExactlyInAnyOrderElementsOf(scan.declaresASchema());

        List<String> problems = new ArrayList<>();
        int checked = 0;

        for (Map.Entry<String, String> pair : entityByUseCase.entrySet()) {
            String useCase = pair.getKey();
            String entity = pair.getValue();
            List<String> fields =
                    ListSchemaScanner.sortableFields(ListSchemaScanner.schemaBlock(scan.sources().get(useCase)));

            assertThat(fields)
                    .withFailMessage("%s declares a ListSchema but no `.sortable(...)` field was "
                            + "parsed out of it. Every schema has at least one (defaultSort must be "
                            + "in the sortable set), so the block regex truncated — and this "
                            + "endpoint's columns went unchecked while the total stayed non-zero.",
                            useCase)
                    .isNotEmpty();

            for (String field : fields) {
                checked++;
                Optional<ListSchemaScanner.FieldDecl> decl =
                        ListSchemaScanner.declaration(scan.sources(), entity, field);
                if (decl.isEmpty()) {
                    problems.add("  - %s sorts by `%s` on %s: no such field on it or its "
                            .formatted(useCase, field, entity)
                            + "superclasses — the sort would fail at query time, not just page badly");
                    continue;
                }
                if (ListSchemaScanner.isNullable(decl.get())) {
                    problems.add("  - %s sorts by `%s` on %s: declared `%s` with no `nullable = false`"
                            .formatted(useCase, field, entity, decl.get().javaType()));
                }
            }
        }

        assertThat(checked)
                .withFailMessage("No sortable fields found across %d list endpoint(s). The scan "
                        + "broke — a check that examines nothing passes for the wrong reason.",
                        entityByUseCase.size())
                .isNotZero();

        assertThat(problems)
                .withFailMessage("""
                        %d sortable field(s) out of %d checked do not map to a non-nullable column:
                        %s
                        The keyset cursor compares against the sort column, and in SQL a NULL \
                        comparison is unknown rather than false — so every row with a null here \
                        DISAPPEARS after page one, silently, with no error and no failing test. \
                        Either make the column NOT NULL, or do not offer it as a sort.""",
                        problems.size(), checked, String.join("\n", problems))
                .isEmpty();
    }
}
