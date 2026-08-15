package com.vointika.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * Every field a list endpoint can be filtered or sorted by is named in that
 * endpoint's section of the API guide.
 *
 * <p><b>Why this test exists.</b> The guide's Lists overview promises it — <i>"The
 * fields and the operators each one allows are listed per endpoint … Each
 * endpoint lists what it sorts by, and its default."</i> — and <b>13 of 15 list
 * endpoints broke that promise.</b> Four named nothing at all: "List Pages" read
 * <i>"Any member. Cursor-paginated."</i> while its schema carried four filters and
 * four sortable fields. The effect is not a wrong statement a reader can catch;
 * it is working functionality nobody can discover, which is why it survived
 * eight audit passes.
 *
 * <p>It drifted because the filter/sort prose is <b>hand-written</b> — only one
 * {@code query-parameters.adoc} snippet exists across all 153 documented
 * operations, so REST Docs never saw these fields and could not check them.
 * {@link ListSchema} is machine-readable, so this closes the loop the generator
 * cannot.
 *
 * <p>{@link #OPERATIONS} is hand-kept on purpose: a new list endpoint fails this
 * test until it is added, which is the moment to also write its section. Reading
 * it from the controller would make the mapping automatic and the omission
 * silent — the failure this test exists to prevent.
 */
class ApiGuideDocumentsEveryListFieldTest {

    private static final Path API_GUIDE = Path.of("src/docs/asciidoc/api-guide.adoc");
    private static final Path MAIN_SOURCES = Path.of("src/main/java");

    /** Use case declaring the schema → the {@code operation::} name of its section. */
    private static final Map<String, String> OPERATIONS = Map.ofEntries(
            Map.entry("ListAudiencesUseCase", "audiences/list"),
            Map.entry("ListAuditLogUseCase", "audit-log/list"),
            Map.entry("ListContactMessagesUseCase", "contact-messages/list"),
            Map.entry("ListExperiencesUseCase", "experiences/list"),
            Map.entry("ListInvitationsUseCase", "tour-operators/invitations/list"),
            Map.entry("ListMediaUseCase", "media/list"),
            Map.entry("ListMembersUseCase", "tour-operators/members/list"),
            Map.entry("ListMenusUseCase", "menus/list"),
            Map.entry("ListMetafieldDefinitionsUseCase", "metafield-definitions/list"),
            Map.entry("ListMetaobjectDefinitionsUseCase", "metaobject-definitions/list"),
            Map.entry("ListMetaobjectEntriesUseCase", "metaobjects/list"),
            Map.entry("ListPagesUseCase", "pages/list"),
            Map.entry("ListPickupLocationsUseCase", "pickup-locations/list"),
            Map.entry("ListPoliciesUseCase", "tour-operators/policies/list"),
            Map.entry("ListSlotsUseCase", "slots/list"));

    private static final Pattern SCHEMA_BLOCK =
            Pattern.compile("SCHEMA\\s*=\\s*ListSchema\\.builder\\(\\)(.*?)\\.build\\(\\)", Pattern.DOTALL);
    private static final Pattern FILTER =
            Pattern.compile("\\.(?:text|set|number|time|instant|bool)\\(\\s*\"([^\"]+)\"");
    private static final Pattern SORTABLE = Pattern.compile("\\.sortable\\(\\s*\"([^\"]+)\"");
    /** The {@code -} is direction, not part of the field name the guide prints. */
    private static final Pattern DEFAULT_SORT = Pattern.compile("\\.defaultSort\\(\\s*\"-?([^\"]+)\"");
    /**
     * The two clauses, each ending at the {@code ;} that closes it. Parsing spans
     * rather than scanning the whole section is what makes the check
     * <b>bidirectional</b>: an exact set comparison needs to know which backticked
     * names are claims about filters, which are claims about sorts, and which
     * (like {@code `cursor`}) are neither.
     */
    private static final Pattern FILTER_CLAUSE = Pattern.compile("[Ff]ilter by\\s+([^;.]+)");
    private static final Pattern SORT_CLAUSE = Pattern.compile("sort by\\s+([^;.]+)");
    private static final Pattern BACKTICKED = Pattern.compile("`([a-zA-Z][a-zA-Z0-9]*)`");

    @Test
    void everyFilterAndSortFieldIsNamedInItsSection() throws IOException {
        Map<String, Schema> schemas = listSchemas();
        String guide = Files.readString(API_GUIDE, StandardCharsets.UTF_8);

        // Both directions, and NOT `isNotEmpty()`. A scan that silently found 3
        // schemas instead of 15 passed every other assertion here — verified by
        // crippling the file walk and watching it stay green. An endpoint this
        // test forgot to look at is indistinguishable from one it approved.
        Set<String> onlyInCode = new TreeSet<>(schemas.keySet());
        onlyInCode.removeAll(OPERATIONS.keySet());
        Set<String> onlyInMap = new TreeSet<>(OPERATIONS.keySet());
        onlyInMap.removeAll(schemas.keySet());

        assertThat(onlyInCode)
                .withFailMessage("""
                        These list schemas have no entry in OPERATIONS, so nothing checks that their \
                        filters and sorts are documented:
                        %s
                        Add the use case → operation mapping, and write the section it points at.""",
                        bullets(onlyInCode))
                .isEmpty();

        assertThat(onlyInMap)
                .withFailMessage("""
                        OPERATIONS names %d use case(s) whose ListSchema this test could not find:
                        %s
                        Either they were renamed or removed — update OPERATIONS — or the scan itself \
                        broke, in which case this test is silently checking fewer endpoints than it \
                        claims. It found %d of the %d it expects.""",
                        onlyInMap.size(), bullets(onlyInMap), schemas.size(), OPERATIONS.size())
                .isEmpty();

        Map<String, Set<String>> wrong = new TreeMap<>();
        schemas.forEach((useCase, schema) -> {
            String operation = OPERATIONS.get(useCase);
            String section = sectionFor(guide, operation);
            String filterClause = clause(FILTER_CLAUSE, section, operation, "filter by");
            String sortClause = clause(SORT_CLAUSE, section, operation, "sort by");

            Set<String> problems = new TreeSet<>();
            difference(schema.filters(), backticked(filterClause), "filter", problems);
            difference(schema.sortable(), backticked(sortClause), "sort", problems);
            defaultSort(schema.defaultSort(), sortClause, problems);
            if (!problems.isEmpty()) {
                wrong.put(useCase, problems);
            }
        });

        assertThat(wrong)
                .withFailMessage("""
                        %d list endpoint(s) whose guide section disagrees with their ListSchema:
                        %s
                        MISSING = the schema allows it and the guide never says so — working \
                        functionality no consumer can find.
                        PHANTOM = the guide promises it and the schema does not allow it — a \
                        consumer who uses it gets a 422.
                        WRONG DEFAULT = the guide marks one field '(default, …)' and defaultSort \
                        orders by another — a consumer gets a page ordered by something other \
                        than what they were told.
                        UNMARKED = the sort clause marks no default at all, which the Lists \
                        overview promises every endpoint states.
                        UNREADABLE = this test could not read the schema's defaultSort, so it is \
                        reporting its own blind spot rather than a fault in the guide.""",
                        wrong.size(), describe(wrong))
                .isEmpty();
    }

    /** What one list endpoint allows, kept apart because the guide states them apart. */
    private record Schema(Set<String> filters, Set<String> sortable, String defaultSort) {}

    /**
     * The field the section marks {@code (default …)} must be the one
     * {@code defaultSort} actually orders by.
     *
     * <p><b>Comparing the sortable sets does not cover this.</b> The default is a
     * member of that set either way, so moving the marker onto the wrong field
     * changes nothing the set comparison can see — verified by mutation: claiming
     * {@code `name` (default, A-Z)} on List Audiences against
     * {@code defaultSort("-createdAt")} left this test green before this check
     * existed. It is also the worse failure of the two. A field the guide omits is
     * functionality a consumer cannot find; a default the guide gets wrong is a
     * statement they will act on, and the page they get back is ordered by
     * something else.
     *
     * <p>The marker is read as the <b>last</b> backticked name before
     * {@code (default}, which is the phrasing every section uses: the default
     * leads the sort clause and carries the parenthetical.
     */
    private static void defaultSort(String schemaDefault, String sortClause, Set<String> into) {
        // A default the scan could not read is not a guide error, and must not be
        // reported as one: `.defaultSort(NEWEST_FIRST)` compiles and behaves
        // identically to the literal, and blaming the section sends whoever reads
        // this to rewrite prose that was already right.
        if (schemaDefault == null) {
            into.add("UNREADABLE defaultSort — the scan found no string literal in "
                    + ".defaultSort(...); the section may well be correct");
            return;
        }
        int marker = sortClause.indexOf("(default");
        if (marker < 0) {
            into.add("UNMARKED default — the schema orders by " + schemaDefault
                    + " and the sort clause marks no field '(default, …)'");
            return;
        }
        List<String> before = allMatches(BACKTICKED, sortClause.substring(0, marker));
        if (before.isEmpty()) {
            into.add("UNMARKED default — '(default' is not preceded by a `field`");
            return;
        }
        String claimed = before.getLast();
        if (!claimed.equals(schemaDefault)) {
            into.add("WRONG DEFAULT — the guide marks " + claimed
                    + " and defaultSort orders by " + schemaDefault);
        }
    }

    /** One clause's text. Absent clause = a failure, never an empty string. */
    private static String clause(Pattern pattern, String section, String operation, String label) {
        Matcher m = pattern.matcher(section);
        assertThat(m.find())
                .withFailMessage("The section for `%s` has no \"%s `field`, …\" clause. Every list "
                        + "section states both, in that phrasing, so the two can be compared against "
                        + "the schema apart. Without it this check cannot run at all — which is worse "
                        + "than failing, because it would pass.", operation, label)
                .isTrue();
        return m.group(1);
    }

    /** The field names a clause claims. */
    private static Set<String> backticked(String clause) {
        return new TreeSet<>(allMatches(BACKTICKED, clause));
    }

    /** Both directions: what the schema allows and the guide omits, and the reverse. */
    private static void difference(Set<String> schema, Set<String> claimedFields,
                                   String kind, Set<String> into) {
        schema.stream().filter(f -> !claimedFields.contains(f))
                .map(f -> "MISSING " + f + " (" + kind + ")").forEach(into::add);
        claimedFields.stream().filter(f -> !schema.contains(f))
                .map(f -> "PHANTOM " + f + " (" + kind + ")").forEach(into::add);
    }

    /** Use case simple name → what its schema allows. */
    private static Map<String, Schema> listSchemas() throws IOException {
        Map<String, Schema> found = new TreeMap<>();
        try (Stream<Path> files = Files.walk(MAIN_SOURCES)) {
            for (Path file : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String source = Files.readString(file, StandardCharsets.UTF_8);
                Matcher block = SCHEMA_BLOCK.matcher(source);
                while (block.find()) {
                    String name = file.getFileName().toString().replace(".java", "");
                    List<String> declaredDefault = allMatches(DEFAULT_SORT, block.group(1));
                    found.put(name, new Schema(
                            new TreeSet<>(allMatches(FILTER, block.group(1))),
                            new TreeSet<>(allMatches(SORTABLE, block.group(1))),
                            declaredDefault.isEmpty() ? null : declaredDefault.getFirst()));
                }
            }
        }
        return found;
    }

    /** The guide text from the {@code ====} heading that carries this operation to the next one. */
    private static String sectionFor(String guide, String operation) {
        int at = guide.indexOf("operation::" + operation + "[");
        assertThat(at)
                .withFailMessage("api-guide.adoc has no `operation::%s[]` line. If the operation was "
                        + "renamed, update OPERATIONS in this test.", operation)
                .isNotNegative();
        int start = guide.lastIndexOf("\n==== ", at);
        return guide.substring(start < 0 ? 0 : start, at);
    }

    private static List<String> allMatches(Pattern pattern, String text) {
        return pattern.matcher(text).results().map(r -> r.group(1)).toList();
    }

    private static String describe(Map<String, Set<String>> wrong) {
        Map<String, Set<String>> ordered = new LinkedHashMap<>(wrong);
        List<String> lines = new ArrayList<>();
        ordered.forEach((useCase, missing) ->
                lines.add("  - " + OPERATIONS.get(useCase) + " (" + useCase + "): "
                        + String.join(", ", missing)));
        return String.join("\n", lines);
    }

    private static String bullets(Set<String> items) {
        return items.stream().map(i -> "  - " + i).reduce((a, b) -> a + "\n" + b).orElse("");
    }
}
