package com.vointika.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every filterable field on a nullable column is declared {@code .nullable(...)}
 * on its {@code ListSchema}, and every field declared there really is nullable.
 *
 * <p><b>The bug this exists to stop.</b> {@code NEQ}, {@code NOT_CONTAINS} and
 * {@code NOT_IN} are built as a negation, and in SQL {@code NOT (NULL LIKE 'x')}
 * is <em>unknown</em> rather than true. {@code WHERE} keeps only true rows, so a
 * row with no value matches neither the filter nor its negation and <b>vanishes
 * from a result it belongs in</b>. Measured on the dev database before the
 * declaration existed: 12 contact messages, one with no name, and
 * {@code filter[name][not_contains]=zzz} returned 11 — a filter that excludes
 * nothing still lost a row. No error, no log line, just a short list.
 *
 * <p><b>Why this is a declaration and not a ban.</b> Sorting on a nullable column
 * is always wrong, so {@link SortableColumnsAreNeverNullableTest} forbids it
 * outright. Filtering is different: {@code contains}, {@code eq} and
 * {@code starts_with} are all correct on a nullable column — a null genuinely
 * does not contain "x". Only the negating operators break, and which operator a
 * caller uses is decided by the request, not by the code. So the schema declares
 * the fact and {@code ListQueryParser} refuses those three at parse time, with a
 * 422 that says why.
 *
 * <p><b>Both directions are checked, and that is the point.</b> Three guards in
 * this codebase passed their own mutation because they only asked "did anything
 * happen":
 * <ul>
 *   <li><b>MISSING</b> — the column is nullable and the schema does not say so.
 *       This is the live bug: the parser allows the operator and rows disappear.
 *   <li><b>PHANTOM</b> — the schema says nullable and the column is
 *       {@code NOT NULL}. Harmless to data, but it 422s a filter that would have
 *       worked, and it is how a declaration rots into a lie once the column is
 *       tightened.
 * </ul>
 *
 * <p>The source scanning lives in {@link ListSchemaScanner}, shared with the
 * sortable guard so a blind spot fixed in one cannot leave the other blind.
 */
class FilterableNullableColumnsAreDeclaredTest {

    @Test
    void everyNullableFilterableColumnIsDeclaredAndEveryDeclarationIsTrue() throws IOException {
        ListSchemaScanner.Scan scan = ListSchemaScanner.scan();
        Map<String, String> entityByUseCase = scan.entityByUseCase();

        assertThat(entityByUseCase.keySet())
                .withFailMessage("""
                        The ListSchema declarations and the entity pairings disagree, so some \
                        endpoint's filters went unchecked.
                        Declares a schema, never paired with an entity: %s
                        Paired with an entity, no schema this test could parse: %s
                        The pairing is read from `listExecutor.list(FooJpaEntity.class, \
                        SomeUseCase.SCHEMA, …)`. Either a list is wired another way — teach the \
                        scanner how to find it — or the scan broke.""",
                        ListSchemaScanner.minus(scan.declaresASchema(), entityByUseCase.keySet()),
                        ListSchemaScanner.minus(entityByUseCase.keySet(), scan.declaresASchema()))
                .containsExactlyInAnyOrderElementsOf(scan.declaresASchema());

        List<String> missing = new ArrayList<>();
        List<String> phantom = new ArrayList<>();
        int checked = 0;

        for (Map.Entry<String, String> pair : entityByUseCase.entrySet()) {
            String useCase = pair.getKey();
            String entity = pair.getValue();
            String block = ListSchemaScanner.schemaBlock(scan.sources().get(useCase));

            List<String> filterFields = ListSchemaScanner.filterFields(block);
            Set<String> declaredNullable = ListSchemaScanner.nullableDeclarations(block);

            for (String field : filterFields) {
                checked++;
                Optional<ListSchemaScanner.FieldDecl> decl =
                        ListSchemaScanner.declaration(scan.sources(), entity, field);
                if (decl.isEmpty()) {
                    missing.add("  - %s: field `%s` not found on %s or its superclasses"
                            .formatted(useCase, field, entity));
                    continue;
                }
                boolean nullable = ListSchemaScanner.isNullable(decl.get());
                if (nullable && !declaredNullable.contains(field)) {
                    missing.add("  - %s.%s (on %s) is nullable and NOT declared"
                            .formatted(useCase, field, entity));
                }
                if (!nullable && declaredNullable.contains(field)) {
                    phantom.add("  - %s.%s (on %s) is declared nullable but the column is NOT NULL"
                            .formatted(useCase, field, entity));
                }
            }
        }

        // Non-vacuity, and deliberately not isNotEmpty() on the problem lists —
        // "did anything happen" is a different question from "did I cover what I claim to".
        assertThat(checked)
                .withFailMessage("No filterable fields found across %d list endpoint(s). The scan "
                        + "broke, and a check that examines nothing passes for the wrong reason.",
                        entityByUseCase.size())
                .isNotZero();

        assertThat(missing)
                .withFailMessage("""
                        %d filterable field(s) sit on a nullable column without declaring it:
                        %s
                        A negating operator (neq, not_contains, not_in) on one of these DROPS every \
                        row with no value — silently, with no error and no failing test. Add \
                        `.nullable("<field>")` to the schema so ListQueryParser refuses those \
                        operators, or make the column NOT NULL.""",
                        missing.size(), String.join("\n", missing))
                .isEmpty();

        assertThat(phantom)
                .withFailMessage("""
                        %d field(s) are declared nullable but their column is NOT NULL:
                        %s
                        Harmless to the data, wrong for the caller: a working filter answers 422 \
                        for a reason that no longer exists. Drop the `.nullable(...)` line.""",
                        phantom.size(), String.join("\n", phantom))
                .isEmpty();
    }
}
