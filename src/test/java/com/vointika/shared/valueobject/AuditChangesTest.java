package com.vointika.shared.valueobject;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AuditChangesTest {

    @Test
    void emitsOnlyChangedFieldsInStableOrder() {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("name", "Old");
        before.put("capacity", 10);
        before.put("tags", List.of("a", "b"));
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("name", "New");
        after.put("capacity", 10);
        after.put("tags", List.of("a", "b", "c"));

        List<FieldChange> changes = AuditChanges.diff(before, after);

        assertThat(changes).containsExactly(
                new FieldChange("name", "Old", "New"),
                new FieldChange("tags", List.of("a", "b"), List.of("a", "b", "c")));
    }

    @Test
    void nullSafeOnBothSidesAndAfterOnlyKeysAppend() {
        Map<String, Object> before = new LinkedHashMap<>();
        before.put("cleared", "value");
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("cleared", null);
        after.put("added", "new");

        assertThat(AuditChanges.diff(before, after)).containsExactly(
                new FieldChange("cleared", "value", null),
                new FieldChange("added", null, "new"));
    }

    @Test
    void identicalSnapshotsDiffEmpty() {
        Map<String, Object> snapshot = Map.of("name", "Same", "featured", true);
        assertThat(AuditChanges.diff(snapshot, new LinkedHashMap<>(snapshot))).isEmpty();
    }
}
