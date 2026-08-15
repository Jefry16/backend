package com.vointika.architecture;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * The source scanner both list-schema guards run on.
 *
 * <p><b>Why it is shared.</b> {@link SortableColumnsAreNeverNullableTest} and
 * {@link FilterableNullableColumnsAreDeclaredTest} ask different questions of the
 * same three facts: which entity a schema lists, what a JPA field declares, and
 * whether that column can be null. They were written a day apart with the
 * scanner copied between them, and a copied scanner is worse here than anywhere
 * else — a blind spot fixed in one guard leaves the other blind to the same
 * entity, so one of the two goes quietly wrong while both stay green. That is
 * the exact failure the "check both directions" reasoning in those guards exists
 * to prevent, one level up.
 *
 * <p>Each guard still owns its own rule and its own failure message. Only the
 * mechanical half is here.
 *
 * <p><b>What it deliberately does not do.</b> It reads what the entity
 * <em>declares</em>, not what the database enforces. An entity that disagrees
 * with its migration is a real bug and a different one.
 */
final class ListSchemaScanner {

    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    /** {@code listExecutor.list(FooJpaEntity.class, ListFoosUseCase.SCHEMA} — the pairing, in code. */
    private static final Pattern EXECUTOR_CALL = Pattern.compile(
            "listExecutor\\.list\\(\\s*(\\w+)\\.class\\s*,\\s*(\\w+)\\.SCHEMA", Pattern.DOTALL);

    private static final Pattern SCHEMA_BLOCK = Pattern.compile(
            "SCHEMA\\s*=\\s*ListSchema\\.builder\\(\\)(.*?)\\.build\\(\\)", Pattern.DOTALL);

    private static final Pattern SORTABLE = Pattern.compile("\\.sortable\\(\\s*\"([^\"]+)\"");
    private static final Pattern FILTER_DECL = Pattern.compile(
            "\\.(set|text|number|instant|time|bool)\\(\\s*\"([^\"]+)\"");
    private static final Pattern NULLABLE_DECL = Pattern.compile("\\.nullable\\(\\s*\"([^\"]+)\"");

    /** {@code @Id} exactly — not {@code @IdClass}, not a custom {@code @IdSomething}. */
    private static final Pattern ID_ANNOTATION = Pattern.compile("@Id\\b(?!\\w)");

    /**
     * One annotation, tolerating <b>one</b> level of nested parentheses.
     *
     * <p>{@code \([^)]*\)} stops at the first {@code )}, so
     * {@code @Column(columnDefinition = "varchar(120)")} truncated mid-annotation
     * and the field's remaining annotations were never seen — including the
     * {@code nullable = false} that decides both guards. One level is enough for
     * every annotation in this codebase and keeps the pattern readable; a second
     * would want a real parser.
     */
    private static final String ANNOTATION = "@[\\w.]+(?:\\((?:[^()]|\\([^()]*\\))*\\))?\\s*";

    /**
     * Keywords that must never be read as a field's type.
     *
     * <p>Making the access modifier optional — needed so a {@code protected} or
     * package-private mapped field resolves — also let {@code TYPE name;} match
     * <em>anywhere</em>, including inside a method body. {@code return name;} in a
     * getter declared above the field then parsed as a declaration of type
     * {@code return} with no annotations, which reads as nullable.
     *
     * <p>It mostly fails loud: empty annotations mean "nullable", so a
     * {@code NOT NULL} column would be reported as an undeclared nullable one.
     * The quiet case is the one that matters — a field already declared
     * {@code .nullable(...)} false-matches to nullable and stays green having
     * never read the real column, so PHANTOM can no longer fire on it.
     *
     * <p>No entity triggers it today (every one declares its fields above its
     * methods), which is what makes it worth closing now rather than after it
     * bites.
     */
    private static final String STATEMENT_KEYWORDS =
            "(?:return|throw|new|else|yield|assert|break|continue|case)";

    private static final Set<String> PRIMITIVES =
            Set.of("int", "long", "boolean", "double", "float", "short", "byte", "char");

    private ListSchemaScanner() {
    }

    /** A JPA field as written: its annotations, and its Java type. */
    record FieldDecl(String annotations, String javaType) {
    }

    /**
     * What one walk over {@code src/main/java} yields.
     *
     * @param entityByUseCase use-case simple name → JPA entity simple name, read from
     *                        the {@code listExecutor.list(...)} call so a new list
     *                        endpoint is covered the day it is written
     * @param declaresASchema every class declaring a {@code SCHEMA = ListSchema.builder()}
     * @param sources         simple class name → file contents
     */
    record Scan(Map<String, String> entityByUseCase,
                Set<String> declaresASchema,
                Map<String, String> sources) {
    }

    /**
     * Both facts come from one walk over FILES, never from the by-simple-name map:
     * several classes share a simple name, so a map-driven pass would silently skip
     * some. Deriving one half from the walk and the other from the map is how the
     * two halves of a guard drift apart.
     */
    static Scan scan() throws IOException {
        Map<String, String> entityByUseCase = new TreeMap<>();
        Set<String> declaresASchema = new TreeSet<>();
        Map<String, String> sources = new HashMap<>();

        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                String name = file.getFileName().toString().replace(".java", "");
                sources.put(name, source);

                Matcher m = EXECUTOR_CALL.matcher(source);
                while (m.find()) {
                    entityByUseCase.put(m.group(2), m.group(1));
                }
                if (SCHEMA_BLOCK.matcher(source).find()) {
                    declaresASchema.add(name);
                }
            }
        }
        return new Scan(entityByUseCase, declaresASchema, sources);
    }

    /** The builder chain between {@code ListSchema.builder()} and {@code .build()}, or empty. */
    static String schemaBlock(String useCaseSource) {
        if (useCaseSource == null) {
            return "";
        }
        Matcher block = SCHEMA_BLOCK.matcher(useCaseSource);
        return block.find() ? block.group(1) : "";
    }

    static List<String> sortableFields(String block) {
        return SORTABLE.matcher(block).results().map(r -> r.group(1)).toList();
    }

    static List<String> filterFields(String block) {
        return FILTER_DECL.matcher(block).results().map(r -> r.group(2)).toList();
    }

    static Set<String> nullableDeclarations(String block) {
        return new TreeSet<>(NULLABLE_DECL.matcher(block).results().map(r -> r.group(1)).toList());
    }

    /**
     * The field as declared on the entity or any superclass, or empty if no class
     * in the chain declares it.
     *
     * <p><b>Follows {@code extends}</b>, because {@code CriteriaListExecutor.fieldType}
     * does — it walks {@code getSuperclass()} until it finds the field. No entity has
     * a base class today, but a {@code @MappedSuperclass} carrying {@code id} and
     * {@code createdAt} is an ordinary thing to add, and a guard that reported "no
     * such field" for working code would be deleted rather than believed.
     *
     * <p><b>Any access modifier, not just {@code private}.</b> The earlier copies
     * matched {@code private} only, so a package-private or {@code protected}
     * mapped field read as "not found" — which one guard reports as a failure and
     * the other would have to guess about.
     */
    static Optional<FieldDecl> declaration(Map<String, String> sources, String entity, String field) {
        String current = entity;
        while (current != null) {
            String source = sources.get(current);
            if (source == null) {
                return Optional.empty();
            }
            Matcher m = Pattern.compile(
                    "((?:" + ANNOTATION + ")*)"
                            + "(?:private|protected|public)?\\s*(?:final\\s+)?(?:transient\\s+)?"
                            // The lookbehind is load-bearing, not belt-and-braces: without
                            // it the engine simply starts one character later and matches
                            // `eturn name;`, so the keyword exclusion never fires. Pinned
                            // by ListSchemaScannerTest.doesNotReadAStatementAsAFieldDeclaration.
                            + "(?<![\\w$.])(?!" + STATEMENT_KEYWORDS + "\\b)"
                            + "([\\w<>\\[\\].]+)\\s+" + Pattern.quote(field) + "\\s*[;=]").matcher(source);
            if (m.find()) {
                return Optional.of(new FieldDecl(m.group(1), m.group(2)));
            }
            Matcher parent = extendsOf(current).matcher(source);
            current = parent.find() ? parent.group(1) : null;
        }
        return Optional.empty();
    }

    /** This class's own superclass — not the first {@code extends} anywhere in the file. */
    private static Pattern extendsOf(String className) {
        return Pattern.compile("class\\s+" + Pattern.quote(className) + "\\b[^{]*?\\bextends\\s+(\\w+)");
    }

    /**
     * Whether the column behind this field can hold a null.
     *
     * <p>A Java primitive cannot, an {@code @Id} must not, and
     * {@code @Column(nullable = false)} says so outright. Everything else is
     * nullable until it says otherwise, which is also JPA's default.
     */
    static boolean isNullable(FieldDecl decl) {
        if (PRIMITIVES.contains(decl.javaType())) {
            return false;
        }
        if (ID_ANNOTATION.matcher(decl.annotations()).find()) {
            return false;
        }
        return !(decl.annotations().contains("nullable = false")
                || decl.annotations().contains("nullable=false"));
    }

    static Set<String> minus(Set<String> all, Set<String> found) {
        Set<String> rest = new TreeSet<>(all);
        rest.removeAll(found);
        return rest;
    }
}
