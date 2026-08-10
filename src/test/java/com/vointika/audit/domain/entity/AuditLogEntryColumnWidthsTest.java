package com.vointika.audit.domain.entity;

import com.vointika.shared.valueobject.AuditActor;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * The entity's length caps and the migration's {@code VARCHAR(n)} are the same
 * number written twice, and nothing made them agree — the audit context's own
 * review flagged three of the four as unpinned.
 *
 * <p>They agree today, which is exactly why this is worth a test: a widened
 * column or a relaxed constant drifts silently, and the failure only appears
 * when a real action is long enough to be truncated or rejected at insert. So
 * this reads the widths **out of the migration** and asserts the entity accepts
 * exactly that many characters and refuses one more. Change either side alone
 * and it fails.
 *
 * <p>The caps themselves are private, deliberately; this pins the behaviour they
 * produce rather than reaching for the fields.
 */
class AuditLogEntryColumnWidthsTest {

    private static final Path MIGRATION =
            Path.of("src/main/resources/db/migration/audit/V1__create_audit_log.sql");

    private static int declaredWidth(String column) throws IOException {
        String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);
        Matcher m = Pattern.compile("^\\s*" + column + "\\s+VARCHAR\\((\\d+)\\)",
                Pattern.MULTILINE | Pattern.CASE_INSENSITIVE).matcher(sql);
        assertThat(m.find())
                .withFailMessage("No VARCHAR(n) declaration for %s in %s — if the column was "
                        + "renamed or retyped, this test must follow it.", column, MIGRATION)
                .isTrue();
        return Integer.parseInt(m.group(1));
    }

    private static void build(String actorName, String entityType, String action, String requestId) {
        AuditLogEntry.record(UUID.randomUUID(), UUID.randomUUID(), AuditActor.system(), actorName,
                entityType, UUID.randomUUID(), action, null, null, requestId, Instant.now());
    }

    @Test
    void entityTypeAcceptsExactlyTheColumnWidth() throws IOException {
        int max = declaredWidth("entity_type");

        assertThatCode(() -> build(null, "x".repeat(max), "a.b", null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> build(null, "x".repeat(max + 1), "a.b", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actionAcceptsExactlyTheColumnWidth() throws IOException {
        int max = declaredWidth("action");

        assertThatCode(() -> build(null, "E", "x".repeat(max), null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> build(null, "E", "x".repeat(max + 1), null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actorNameAcceptsExactlyTheColumnWidth() throws IOException {
        int max = declaredWidth("actor_name");

        assertThatCode(() -> build("x".repeat(max), "E", "a.b", null)).doesNotThrowAnyException();
        assertThatThrownBy(() -> build("x".repeat(max + 1), "E", "a.b", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** The one that was already pinned by being public; kept beside its three siblings. */
    @Test
    void requestIdAcceptsExactlyTheColumnWidth() throws IOException {
        int max = declaredWidth("request_id");

        assertThat(AuditLogEntry.REQUEST_ID_MAX).isEqualTo(max);
        assertThatCode(() -> build(null, "E", "a.b", "x".repeat(max))).doesNotThrowAnyException();
        assertThatThrownBy(() -> build(null, "E", "a.b", "x".repeat(max + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
