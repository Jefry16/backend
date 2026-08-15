package com.vointika.architecture;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The scanner two guards depend on, pinned against synthetic sources.
 *
 * <p><b>Why this exists.</b> Extracting {@link ListSchemaScanner} made it the
 * single point of failure for {@link SortableColumnsAreNeverNullableTest} and
 * {@link FilterableNullableColumnsAreDeclaredTest} — and its behaviour was
 * verified once, by hand, leaving no trace in the build. Edit {@code ANNOTATION}
 * or {@code declaration()} later and the blind spots the extraction closed reopen
 * silently, with both guards still green because neither can tell "this column is
 * fine" from "I could not read this column".
 *
 * <p>Synthetic sources rather than real entities on purpose: a real entity would
 * make this a second copy of the guards, and it would stop testing the parser the
 * day someone reformats the entity.
 */
class ListSchemaScannerTest {

    /** The trap that motivated the nested-paren tolerance. */
    @Test
    void readsNullableFalseAfterAnAnnotationContainingParentheses() {
        Map<String, String> sources = Map.of("Foo", """
                class Foo {
                    @Column(columnDefinition = "varchar(120)", nullable = false)
                    private String name;
                }
                """);

        ListSchemaScanner.FieldDecl decl = ListSchemaScanner.declaration(sources, "Foo", "name").orElseThrow();

        assertThat(decl.javaType()).isEqualTo("String");
        assertThat(ListSchemaScanner.isNullable(decl))
                .withFailMessage("`nullable = false` sits after `varchar(120)`; an annotation "
                        + "pattern that stops at the first ')' loses it and calls the column nullable")
                .isFalse();
    }

    @Test
    void resolvesFieldsWhateverTheAccessModifier() {
        Map<String, String> sources = Map.of("Foo", """
                class Foo {
                    @Column(nullable = false) private String a;
                    @Column(nullable = false) protected String b;
                    @Column(nullable = false) String c;
                }
                """);

        for (String field : new String[]{"a", "b", "c"}) {
            assertThat(ListSchemaScanner.declaration(sources, "Foo", field))
                    .withFailMessage("field `%s` did not resolve", field)
                    .isPresent();
        }
    }

    /**
     * The regression the optional modifier introduced: {@code return name;} in a
     * getter declared above the field parsed as a field of type {@code return}.
     */
    @Test
    void doesNotReadAStatementAsAFieldDeclaration() {
        Map<String, String> sources = Map.of("Foo", """
                class Foo {
                    public String getName() {
                        return name;
                    }

                    @Column(nullable = false)
                    private String name;
                }
                """);

        ListSchemaScanner.FieldDecl decl = ListSchemaScanner.declaration(sources, "Foo", "name").orElseThrow();

        assertThat(decl.javaType())
                .withFailMessage("matched `return name;` instead of the real declaration")
                .isEqualTo("String");
        assertThat(ListSchemaScanner.isNullable(decl))
                .withFailMessage("a false match reads as nullable because it carries no annotations, "
                        + "which silently disables the PHANTOM half of the filter guard")
                .isFalse();
    }

    /**
     * The sibling of the case above, and the one a keyword exclusion cannot
     * reach: a local variable declaration is not a keyword. Each of these
     * false-matched to a type with no annotations, which reads as nullable —
     * so a field already declared {@code .nullable(...)} would stay green
     * having never read the real column.
     */
    @Test
    void doesNotReadALocalVariableAsAFieldDeclaration() {
        String[] locals = {
                "String name = compute();",
                "String name;",
                "var name = 1;",
                "Map<String, String> name = q();",
        };

        for (String local : locals) {
            Map<String, String> sources = Map.of("Foo", """
                    class Foo {
                        public String build() {
                            %s
                            return "x";
                        }

                        @Column(nullable = false)
                        private Instant name;
                    }
                    """.formatted(local));

            ListSchemaScanner.FieldDecl decl =
                    ListSchemaScanner.declaration(sources, "Foo", "name").orElseThrow();

            assertThat(decl.javaType())
                    .withFailMessage("`%s` matched instead of the real field declaration", local)
                    .isEqualTo("Instant");
            assertThat(ListSchemaScanner.isNullable(decl))
                    .withFailMessage("`%s` false-matched and, carrying no annotations, read as nullable", local)
                    .isFalse();
        }
    }

    /**
     * The rule that closes both cases above, stated directly: a declaration has
     * to begin with an annotation or an access modifier. A mapped field with
     * neither reads as "not found", which both guards report as a failure —
     * the loud direction, and preferable to a silent false match.
     */
    @Test
    void requiresAnAnnotationOrAnAccessModifier() {
        Map<String, String> bare = Map.of("Foo", "class Foo {\n    String name;\n}\n");

        assertThat(ListSchemaScanner.declaration(bare, "Foo", "name")).isEmpty();

        Map<String, String> annotated = Map.of("Foo", "class Foo {\n    @Column String name;\n}\n");
        assertThat(ListSchemaScanner.declaration(annotated, "Foo", "name")).isPresent();
    }

    @Test
    void followsExtendsToASuperclass() {
        Map<String, String> sources = Map.of(
                "Child", "class Child extends Base {\n    private String own;\n}\n",
                "Base", "class Base {\n    @Column(nullable = false)\n    private Instant createdAt;\n}\n");

        assertThat(ListSchemaScanner.declaration(sources, "Child", "createdAt")).isPresent();
    }

    /** Anchored to the class's own name, not the first `extends` in the file. */
    @Test
    void followsTheRightSuperclassWhenTheFileHasSeveralClasses() {
        Map<String, String> sources = Map.of(
                "Child", """
                        class Other extends WrongBase {
                        }

                        class Child extends Base {
                        }
                        """,
                "WrongBase", "class WrongBase {\n    private String createdAt;\n}\n",
                "Base", "class Base {\n    @Column(nullable = false)\n    private Instant createdAt;\n}\n");

        ListSchemaScanner.FieldDecl decl =
                ListSchemaScanner.declaration(sources, "Child", "createdAt").orElseThrow();

        assertThat(decl.javaType()).isEqualTo("Instant");
    }

    @Test
    void treatsPrimitivesAndTheIdAsNeverNull() {
        Map<String, String> sources = Map.of("Foo", """
                class Foo {
                    private int count;
                    @Id
                    private UUID id;
                    private String loose;
                }
                """);

        assertThat(ListSchemaScanner.isNullable(decl(sources, "count"))).isFalse();
        assertThat(ListSchemaScanner.isNullable(decl(sources, "id"))).isFalse();
        assertThat(ListSchemaScanner.isNullable(decl(sources, "loose")))
                .withFailMessage("an unannotated object field is nullable — JPA's own default")
                .isTrue();
    }

    /** {@code @IdClass} is not {@code @Id}; treating it as one would hide a nullable column. */
    @Test
    void doesNotMistakeIdClassForId() {
        Map<String, String> sources = Map.of("Foo", """
                class Foo {
                    @IdClass(FooKey.class)
                    private String name;
                }
                """);

        assertThat(ListSchemaScanner.isNullable(decl(sources, "name"))).isTrue();
    }

    @Test
    void returnsEmptyWhenNoClassInTheChainDeclaresTheField() {
        Map<String, String> sources = Map.of("Foo", "class Foo {\n    private String a;\n}\n");

        assertThat(ListSchemaScanner.declaration(sources, "Foo", "missing")).isEmpty();
        assertThat(ListSchemaScanner.declaration(sources, "NoSuchEntity", "a")).isEmpty();
    }

    @Test
    void parsesTheSchemaBuilderChain() {
        String source = """
                public static final ListSchema SCHEMA = ListSchema.builder()
                        .tenantScoped()
                        .text("actorName")
                        .set("actorId", UUID.class)
                        .nullable("actorName")
                        .sortable("id")
                        .defaultSort("-id")
                        .build();
                """;
        String block = ListSchemaScanner.schemaBlock(source);

        assertThat(ListSchemaScanner.filterFields(block)).containsExactly("actorName", "actorId");
        assertThat(ListSchemaScanner.nullableDeclarations(block)).containsExactly("actorName");
        assertThat(ListSchemaScanner.sortableFields(block)).containsExactly("id");
    }

    @Test
    void yieldsAnEmptyBlockRatherThanThrowingWhenThereIsNoSchema() {
        assertThat(ListSchemaScanner.schemaBlock("class Foo {}")).isEmpty();
        assertThat(ListSchemaScanner.schemaBlock(null)).isEmpty();
    }

    private static ListSchemaScanner.FieldDecl decl(Map<String, String> sources, String field) {
        Optional<ListSchemaScanner.FieldDecl> found =
                ListSchemaScanner.declaration(sources, "Foo", field);
        return found.orElseThrow(() -> new AssertionError("field `" + field + "` did not resolve"));
    }
}
