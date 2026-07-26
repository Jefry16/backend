package com.vointika.audit.domain.entity;

import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuditLogEntryTest {

    private static final UUID ID = UUID.randomUUID();
    private static final UUID OP = UUID.randomUUID();
    private static final UUID ENTITY = UUID.randomUUID();
    private static final UUID USER = UUID.randomUUID();

    private AuditLogEntry entry(List<FieldChange> changes) {
        return AuditLogEntry.record(ID, OP, AuditActor.user(USER), "Maria",
                "EXPERIENCE", ENTITY, "experience.updated",
                Map.of("k", "v"), changes, "req-1", Instant.now());
    }

    @Test
    void emptyChangesNormalizeToNull() {
        assertThat(entry(List.of()).getChanges()).isNull();
        assertThat(entry(null).getChanges()).isNull();
        assertThat(entry(List.of(new FieldChange("name", "a", "b"))).getChanges()).hasSize(1);
    }

    @Test
    void blankOrOversizedIdentityFieldsAreRejected() {
        assertThatThrownBy(() -> AuditLogEntry.record(ID, OP, AuditActor.user(USER), null,
                " ", ENTITY, "x.y", null, null, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditLogEntry.record(ID, OP, AuditActor.user(USER), null,
                "EXPERIENCE", ENTITY, "a".repeat(81), null, null, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> AuditLogEntry.record(ID, OP, AuditActor.user(USER), null,
                "EXPERIENCE", ENTITY, "x.y", null, null, "r".repeat(65), Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void actorNameIsOptionalButBounded() {
        assertThat(entry(null).getActorName()).isEqualTo("Maria");
        assertThatThrownBy(() -> AuditLogEntry.record(ID, OP, AuditActor.user(USER), "n".repeat(256),
                "EXPERIENCE", ENTITY, "x.y", null, null, null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
