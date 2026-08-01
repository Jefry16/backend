package com.vointika.audit.infrastructure.persistence;

import com.vointika.shared.valueobject.AuditActorType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@link AuditActorType} enum and the {@code audit_log_actor_type_check}
 * constraint are two lists that must agree, and nothing used to make them.
 *
 * <p>The enum's own javadoc promises that adding {@code STOREFRONT} is "a one-line
 * CHECK-constraint migration" — a promise enforced by nothing. Add the value and
 * forget the migration and the constraint rejects the insert with SQLSTATE 23514,
 * which {@code SpringTransactionRunner} does not translate (it translates only
 * 23505). So it surfaces as a 500 — and because {@code AuditTrailPortImpl} appends
 * inside the caller's transaction on purpose, **the business action it was
 * recording rolls back with it**. The first shopper checkout would fail, not the
 * build.
 *
 * <p>Verified against the running database before this test was written: inserting
 * {@code actor_type = 'STOREFRONT'} today fails with
 * {@code violates check constraint "audit_log_actor_type_check"}.
 */
class ActorTypesMatchTheCheckConstraintTest {

    private static final Path MIGRATIONS = Path.of("src/main/resources/db/migration/audit");

    private static final Pattern VERSION = Pattern.compile("^V(\\d+)__");

    /** The value list of the last statement that defines the actor_type check. */
    private static final Pattern ACTOR_TYPE_CHECK =
            Pattern.compile("actor_type\\s+IN\\s*\\(([^)]*)\\)", Pattern.CASE_INSENSITIVE);

    @Test
    void everyActorTypeIsAcceptedByTheCheckConstraint() throws IOException {
        Set<String> allowed = allowedActorTypes();

        assertThat(allowed)
                .withFailMessage("No actor_type CHECK found in %s — this test cannot see the "
                        + "constraint it exists to track. Fix the parser, do not delete the test.",
                        MIGRATIONS)
                .isNotEmpty();

        for (AuditActorType type : AuditActorType.values()) {
            assertThat(allowed)
                    .withFailMessage(
                            "AuditActorType declares '%s' but the audit_log_actor_type_check "
                                    + "constraint only allows %s. Ship a migration widening the "
                                    + "constraint — otherwise every audited action by a '%s' actor "
                                    + "fails with SQLSTATE 23514, which nothing translates, and takes "
                                    + "the business action down with it (the append shares its "
                                    + "transaction by design).",
                            type, allowed, type)
                    .contains(type.name());
        }
    }

    /**
     * Reads the migrations in version order and keeps the <em>last</em> actor_type
     * check it finds, so a later migration that replaces the constraint wins over
     * the one V1 created. Union-ing them would let a narrowing migration pass.
     *
     * <p>Ordered on the parsed version, not the filename: lexicographically
     * {@code V10} sorts before {@code V1}, which would silently keep the wrong
     * constraint once the history is long enough — the very hole this method's
     * ordering exists to close. A filename that is not {@code V<int>__name.sql}
     * throws here rather than mis-sorting quietly.
     */
    private static Set<String> allowedActorTypes() throws IOException {
        List<Path> files;
        try (Stream<Path> paths = Files.list(MIGRATIONS)) {
            files = paths.filter(p -> p.toString().endsWith(".sql"))
                    .sorted(Comparator.comparingInt(ActorTypesMatchTheCheckConstraintTest::version))
                    .toList();
        }

        Set<String> latest = new LinkedHashSet<>();
        for (Path file : files) {
            Matcher matcher = ACTOR_TYPE_CHECK.matcher(Files.readString(file, StandardCharsets.UTF_8));
            List<String> inThisFile = new ArrayList<>();
            while (matcher.find()) {
                inThisFile.add(matcher.group(1));
            }
            if (!inThisFile.isEmpty()) {
                latest.clear();
                for (String value : inThisFile.getLast().split(",")) {
                    latest.add(value.trim().replace("'", ""));
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
