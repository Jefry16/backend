package com.vointika.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
 * is always wrong, so {@code SortableColumnsAreNeverNullableTest} forbids it
 * outright. Filtering is different: {@code contains}, {@code eq} and
 * {@code starts_with} are all correct on a nullable column — a null genuinely
 * does not contain "x". Only the negating operators break, and which operator a
 * caller uses is decided by the request, not by the code. So the schema declares
 * the fact and {@code ListQueryParser} refuses the three operators at parse
 * time, with a 422 that says why.
 *
 * <p><b>Both directions are checked, and that is the point.</b> Three guards
 * written in this codebase passed their own mutation because they only asked
 * "did anything happen":
 * <ul>
 *   <li><b>MISSING</b> — the column is nullable and the schema does not say so.
 *       This is the live bug: the parser allows the operator and rows disappear.
 *   <li><b>PHANTOM</b> — the schema says nullable and the column is
 *       {@code NOT NULL}. Harmless to data, but it 422s a filter that would have
 *       worked, and it is how a declaration rots into a lie once the column is
 *       tightened.
 * </ul>
 *
 * <p><b>Known limit, stated rather than implied.</b> Like the sortable guard,
 * this reads what the entity declares ({@code @Column(nullable = false)}), not
 * what the database enforces. An entity that disagrees with its migration is a
 * real bug and a different one.
 */
class FilterableNullableColumnsAreDeclaredTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    /** {@code listExecutor.list(FooJpaEntity.class, ListFoosUseCase.SCHEMA} — the pairing, in code. */
    private static final Pattern EXECUTOR_CALL = Pattern.compile(
            "listExecutor\\.list\\(\\s*(\\w+)\\.class\\s*,\\s*(\\w+)\\.SCHEMA", Pattern.DOTALL);

    private static final Pattern SCHEMA_BLOCK = Pattern.compile(
            "SCHEMA\\s*=\\s*ListSchema\\.builder\\(\\)(.*?)\\.build\\(\\)", Pattern.DOTALL);
    private static final Pattern FILTER_DECL = Pattern.compile(
            "\\.(set|text|number|instant|time|bool)\\(\\s*\"([^\"]+)\"");
    private static final Pattern NULLABLE_DECL = Pattern.compile(
            "\\.nullable\\(\\s*\"([^\"]+)\"");

    /** {@code @Id} exactly — not {@code @IdClass}, not a custom {@code @IdSomething}. */
    private static final Pattern ID_ANNOTATION = Pattern.compile("@Id\\b(?!\\w)");

    private static final Set<String> PRIMITIVES =
            Set.of("int", "long", "boolean", "double", "float", "short", "byte", "char");

    @Test
    void everyNullableFilterableColumnIsDeclaredAndEveryDeclarationIsTrue() throws IOException {
        Map<String, String> sources = mainSources();
        Map<String, String> entityByUseCase = new TreeMap<>();
        Set<String> declaresASchema = new TreeSet<>();

        // Both facts come from one walk over FILES, not from `sources`: that map is
        // keyed by simple name and several classes share one, so some would never be
        // read. Deriving one half from the walk and the other from the map is how the
        // two halves of a check like this drift apart.
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher m = EXECUTOR_CALL.matcher(source);
                while (m.find()) {
                    entityByUseCase.put(m.group(2), m.group(1));
                }
                if (SCHEMA_BLOCK.matcher(source).find()) {
                    declaresASchema.add(file.getFileName().toString().replace(".java", ""));
                }
            }
        }

        assertThat(entityByUseCase.keySet())
                .withFailMessage("""
                        The ListSchema declarations and the entity pairings disagree, so some \
                        endpoint's filters went unchecked.
                        Declares a schema, never paired with an entity: %s
                        Paired with an entity, no schema this test could parse: %s
                        The pairing is read from `listExecutor.list(FooJpaEntity.class, \
                        SomeUseCase.SCHEMA, …)`. Either a list is wired another way — teach this \
                        test how to find it — or the scan broke.""",
                        minus(declaresASchema, entityByUseCase.keySet()),
                        minus(entityByUseCase.keySet(), declaresASchema))
                .containsExactlyInAnyOrderElementsOf(declaresASchema);

        List<String> missing = new ArrayList<>();
        List<String> phantom = new ArrayList<>();
        int checked = 0;

        for (Map.Entry<String, String> pair : entityByUseCase.entrySet()) {
            String useCase = pair.getKey();
            String entity = pair.getValue();
            String block = schemaBlock(sources.get(useCase));

            List<String> filterFields = FILTER_DECL.matcher(block).results()
                    .map(r -> r.group(2)).toList();
            Set<String> declaredNullable = new TreeSet<>(NULLABLE_DECL.matcher(block).results()
                    .map(r -> r.group(1)).toList());

            for (String field : filterFields) {
                checked++;
                Boolean nullable = isNullable(sources, entity, field);
                if (nullable == null) {
                    missing.add("  - %s: field `%s` not found on %s or its superclasses"
                            .formatted(useCase, field, entity));
                    continue;
                }
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

        // Non-vacuity. Deliberately not isNotEmpty() on the problem lists — "did
        // anything happen" is a different question from "did I cover what I claim to".
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

    /** True/false, or null when the field is not on the entity at all. */
    private static Boolean isNullable(Map<String, String> sources, String entity, String field) {
        String current = entity;
        while (current != null) {
            String source = sources.get(current);
            if (source == null) {
                return null;
            }
            Matcher m = declaration(source, field);
            if (m != null) {
                String annotations = m.group(1);
                String javaType = m.group(2);
                if (PRIMITIVES.contains(javaType)) {
                    return false;
                }
                if (ID_ANNOTATION.matcher(annotations).find()) {
                    return false;
                }
                return !(annotations.contains("nullable = false") || annotations.contains("nullable=false"));
            }
            Matcher parent = extendsOf(current).matcher(source);
            current = parent.find() ? parent.group(1) : null;
        }
        return null;
    }

    /** This class's own superclass — not the first {@code extends} anywhere in the file. */
    private static Pattern extendsOf(String className) {
        return Pattern.compile("class\\s+" + Pattern.quote(className) + "\\b[^{]*?\\bextends\\s+(\\w+)");
    }

    private static Matcher declaration(String source, String field) {
        Matcher m = Pattern.compile(
                "((?:@[\\w.]+(?:\\([^)]*\\))?\\s*)*)private\\s+([\\w<>\\[\\].]+)\\s+"
                        + Pattern.quote(field) + "\\s*[;=]").matcher(source);
        return m.find() ? m : null;
    }

    private static String schemaBlock(String useCaseSource) {
        Matcher block = SCHEMA_BLOCK.matcher(useCaseSource);
        return block.find() ? block.group(1) : "";
    }

    private static Map<String, String> mainSources() throws IOException {
        Map<String, String> sources = new HashMap<>();
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                sources.put(file.getFileName().toString().replace(".java", ""),
                        Files.readString(file, StandardCharsets.UTF_8));
            }
        }
        return sources;
    }

    private static Set<String> minus(Set<String> all, Set<String> found) {
        Set<String> rest = new TreeSet<>(all);
        rest.removeAll(found);
        return rest;
    }
}
