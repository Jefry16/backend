package com.vointika.touroperator.infrastructure.persistence;

import com.vointika.touroperator.domain.enums.BrandColorRole;
import com.vointika.touroperator.domain.enums.BrandSocialPlatform;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link BrandSocialPlatform} and {@link BrandColorRole} each pair with a CHECK
 * constraint, and nothing but this makes them agree.
 *
 * <p>This project has already shipped that drift once — {@code AuditActorType}
 * against {@code audit_log_actor_type_check} — and an audit caught it rather
 * than the build. The failure mode is the same here: add a platform, forget the
 * migration, and the insert fails with SQLSTATE 23514, which
 * {@code SpringTransactionRunner} does not translate (it translates only 23505).
 * It surfaces as a 500 on whatever write path lands first.
 *
 * <p>Both columns are also mapped {@code @Enumerated(STRING)}, so the coupling
 * runs the other way too: a value in the database that the enum does not have
 * fails the <em>read</em> — on a public storefront page. That is why the two
 * lists are asserted <b>equal</b> rather than the enum being a subset.
 */
class BrandEnumsMatchTheCheckConstraintsTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration/touroperator");

    private static final Pattern VERSION = Pattern.compile("^V(\\d+)__");

    @Test
    void everySocialPlatformIsAcceptedByTheCheckConstraint() throws IOException {
        assertEnumMatchesCheck("platform", BrandSocialPlatform.class,
                "tour_operator_brand_social_links");
    }

    @Test
    void everyColorRoleIsAcceptedByTheCheckConstraint() throws IOException {
        assertEnumMatchesCheck("role", BrandColorRole.class, "tour_operator_brand_colors");
    }

    private static void assertEnumMatchesCheck(String column, Class<? extends Enum<?>> type, String table)
            throws IOException {
        Set<String> allowed = allowedValues(column);

        assertThat(allowed)
                .withFailMessage("No %s CHECK found in %s — this test cannot see the constraint it "
                        + "exists to track. Fix the parser, do not delete the test.", column, MIGRATIONS)
                .isNotEmpty();

        Set<String> declared = Arrays.stream(type.getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        assertThat(allowed)
                .withFailMessage(
                        "%s declares %s but the %s.%s constraint allows %s, and the two must match "
                                + "exactly — the coupling runs both ways:%n"
                                + "  in the enum, not in the CHECK -> every insert of that value fails "
                                + "with SQLSTATE 23514, which nothing translates (only 23505 is)%n"
                                + "  in the CHECK, not in the enum -> the column is mapped "
                                + "@Enumerated(STRING), so a row carrying it fails the READ, on a public "
                                + "storefront page%n"
                                + "Ship the migration and the enum change together.",
                        type.getSimpleName(), declared, table, column, allowed)
                .containsExactlyInAnyOrderElementsOf(declared);
    }

    /**
     * Reads the migrations in version order and keeps the <em>last</em> check it
     * finds for the column, so a later migration that replaces the constraint wins
     * over the one that created it. Union-ing them would let a narrowing migration
     * pass.
     *
     * <p>Ordered on the parsed version, not the filename: lexicographically
     * {@code V10} sorts before {@code V1}, which would silently keep the wrong
     * constraint — and this context is already past V10, so that is live rather
     * than hypothetical.
     */
    private static Set<String> allowedValues(String column) throws IOException {
        Pattern check = Pattern.compile(column + "\\s+IN\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);
        List<Path> files;
        try (Stream<Path> paths = Files.list(MIGRATIONS)) {
            files = paths.filter(p -> p.toString().endsWith(".sql"))
                    .sorted(Comparator.comparingInt(BrandEnumsMatchTheCheckConstraintsTest::version))
                    .toList();
        }

        Set<String> latest = new LinkedHashSet<>();
        for (Path file : files) {
            Matcher matcher = check.matcher(Files.readString(file, StandardCharsets.UTF_8));
            List<String> inThisFile = new ArrayList<>();
            while (matcher.find()) {
                inThisFile.add(matcher.group(1));
            }
            if (!inThisFile.isEmpty()) {
                latest.clear();
                for (String value : inThisFile.getLast().split(",")) {
                    latest.add(value.trim().replace("'", "").replace("\n", ""));
                }
            }
        }
        return latest;
    }

    private static int version(Path migration) {
        String name = migration.getFileName().toString();
        Matcher matcher = VERSION.matcher(name);
        if (!matcher.find()) {
            throw new IllegalStateException(
                    "Migration '" + name + "' is not V<int>__name.sql, so this test cannot order it "
                            + "against the others. Teach the parser the new scheme — silently "
                            + "mis-ordering would let a narrowing constraint through.");
        }
        return Integer.parseInt(matcher.group(1));
    }
}
