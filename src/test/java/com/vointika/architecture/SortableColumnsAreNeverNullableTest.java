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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

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
 * <p><b>The endpoint→table pairing is derived, not hand-kept</b>, because the
 * repository already states both in one call:
 * {@code listExecutor.list(FooJpaEntity.class, ListFoosUseCase.SCHEMA, …)}. A new
 * list endpoint is therefore covered the day it is written rather than when
 * someone remembers to register it here.
 *
 * <p><b>Known limit, stated rather than implied.</b> This reads what the
 * application declares ({@code @Column(nullable = false)}), not what the database
 * enforces. An entity that disagrees with its migration is a real bug, and a
 * different one from this.
 */
class SortableColumnsAreNeverNullableTest {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    /** {@code listExecutor.list(FooJpaEntity.class, ListFoosUseCase.SCHEMA} — the pairing, in code. */
    private static final Pattern EXECUTOR_CALL = Pattern.compile(
            "listExecutor\\.list\\(\\s*(\\w+)\\.class\\s*,\\s*(\\w+)\\.SCHEMA", Pattern.DOTALL);

    private static final Pattern SCHEMA_BLOCK = Pattern.compile(
            "SCHEMA\\s*=\\s*ListSchema\\.builder\\(\\)(.*?)\\.build\\(\\)", Pattern.DOTALL);
    private static final Pattern SORTABLE = Pattern.compile("\\.sortable\\(\\s*\"([^\"]+)\"");

    /** {@code @Id} exactly — not {@code @IdClass} and not a custom {@code @IdSomething}. */
    private static final Pattern ID_ANNOTATION = Pattern.compile("@Id\\b(?!\\w)");

    private static final Set<String> PRIMITIVES =
            Set.of("int", "long", "boolean", "double", "float", "short", "byte", "char");

    @Test
    void everySortableFieldMapsToANonNullableColumn() throws IOException {
        Map<String, String> sources = mainSources();
        Map<String, String> entityByUseCase = new TreeMap<>();
        Set<String> declared = new java.util.TreeSet<>();
        // Both facts come from the same walk over FILES, not from `sources`: that map
        // is keyed by simple name and three classes are called UseCaseConfig, so two
        // of them would never be read. Deriving one from the walk and the other from
        // the map is how the two halves of this check drift apart.
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher m = EXECUTOR_CALL.matcher(source);
                while (m.find()) {
                    entityByUseCase.put(m.group(2), m.group(1));
                }
                if (SCHEMA_BLOCK.matcher(source).find()) {
                    declared.add(file.getFileName().toString().replace(".java", ""));
                }
            }
        }

        // Non-vacuity, and deliberately not `isNotEmpty()`. A schema that declares
        // sortable fields but is never wired to the executor has to FAIL here, not
        // be quietly skipped — "did anything happen" is not the same question as
        // "did I cover everything I claim to".
        // Reported in both directions. Subtracting the sizes was tempting and wrong:
        // a use case paired but with no parsed schema makes the count negative and
        // the list empty, so a real failure would print "0 class(es):" and nothing.
        Set<String> unpaired = minus(declared, entityByUseCase.keySet());
        Set<String> unparsed = minus(entityByUseCase.keySet(), declared);
        assertThat(entityByUseCase.keySet())
                .withFailMessage("""
                        The ListSchema declarations and the entity pairings disagree, so some \
                        endpoint's sortable columns went unchecked.
                        Declares a ListSchema, never paired with an entity (%d):
                        %s
                        Paired with an entity, no ListSchema this test could parse (%d):
                        %s
                        The pairing is read from `listExecutor.list(FooJpaEntity.class, \
                        SomeUseCase.SCHEMA, …)`. Either a list is wired some other way — teach this \
                        test how to find it — or the scan broke.""",
                        unpaired.size(), bullets(unpaired), unparsed.size(), bullets(unparsed))
                .containsExactlyInAnyOrderElementsOf(declared);

        List<String> problems = new ArrayList<>();
        int checked = 0;
        for (Map.Entry<String, String> pair : entityByUseCase.entrySet()) {
            String useCase = pair.getKey();
            String entity = pair.getValue();
            List<String> fields = sortableFields(sources.get(useCase));
            assertThat(fields)
                    .withFailMessage("%s declares a ListSchema but no `.sortable(...)` field was "
                            + "parsed out of it. Every schema has at least one (defaultSort must be "
                            + "in the sortable set), so the block regex truncated — and this "
                            + "endpoint's columns went unchecked while the total stayed non-zero.",
                            useCase)
                    .isNotEmpty();
            for (String field : fields) {
                checked++;
                String verdict = nullability(sources, entity, field);
                if (verdict != null) {
                    problems.add("  - %s sorts by `%s` on %s: %s".formatted(useCase, field, entity, verdict));
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

    /** This class's own superclass — not the first {@code extends} anywhere in the file. */
    private static Pattern extendsOf(String className) {
        return Pattern.compile("class\\s+" + Pattern.quote(className) + "\\b[^{]*?\\bextends\\s+(\\w+)");
    }

    /**
     * Null when the field is safe; otherwise why it is not.
     *
     * <p><b>Follows {@code extends}</b>, because {@code CriteriaListExecutor.fieldType}
     * does — it walks {@code getSuperclass()} until it finds the field. No entity
     * has a base class today, but a {@code @MappedSuperclass} carrying {@code id}
     * and {@code createdAt} is an ordinary thing to add, and a guard that reported
     * "no such field" for working code would be deleted rather than believed.
     */
    private static String nullability(Map<String, String> sources, String entity, String field) {
        String current = entity;
        Matcher m = null;
        while (current != null) {
            String source = sources.get(current);
            if (source == null) {
                return "source for `" + current + "` not found — this test cannot see the column at all";
            }
            m = declaration(source, field);
            if (m != null) {
                break;
            }
            Matcher parent = extendsOf(current).matcher(source);
            current = parent.find() ? parent.group(1) : null;
        }
        if (m == null) {
            return "no field named `" + field + "` on " + entity + " or its superclasses"
                    + " — the sort would fail at query time, not just page badly";
        }
        String annotations = m.group(1);
        String javaType = m.group(2);
        if (PRIMITIVES.contains(javaType)) {
            return null;
        }
        if (ID_ANNOTATION.matcher(annotations).find()) {
            return null;
        }
        if (annotations.contains("nullable = false") || annotations.contains("nullable=false")) {
            return null;
        }
        return "declared `" + javaType + "` with no `nullable = false`";
    }

    /** The field's declaration with its annotations, or null if this class does not declare it. */
    private static Matcher declaration(String source, String field) {
        Matcher m = Pattern.compile(
                "((?:@[\\w.]+(?:\\([^)]*\\))?\\s*)*)private\\s+([\\w<>\\[\\].]+)\\s+"
                        + Pattern.quote(field) + "\\s*[;=]").matcher(source);
        return m.find() ? m : null;
    }

    private static List<String> sortableFields(String useCaseSource) {
        Matcher block = SCHEMA_BLOCK.matcher(useCaseSource);
        if (!block.find()) {
            return List.of();
        }
        return SORTABLE.matcher(block.group(1)).results().map(r -> r.group(1)).toList();
    }

    /** Simple class name → source, for every file under {@code src/main/java}. */
    private static Map<String, String> mainSources() throws IOException {
        Map<String, String> sources = new HashMap<>();
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String name = file.getFileName().toString().replace(".java", "");
                sources.put(name, Files.readString(file, StandardCharsets.UTF_8));
            }
        }
        return sources;
    }

    private static Set<String> minus(Set<String> all, Set<String> found) {
        Set<String> rest = new java.util.TreeSet<>(all);
        rest.removeAll(found);
        return rest;
    }

    private static String bullets(Set<String> items) {
        return items.stream().map(i -> "  - " + i).reduce((a, b) -> a + "\n" + b).orElse("");
    }
}
