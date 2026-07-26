package com.vointika.shared.valueobject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Computes the field-level diff between two domain snapshots — the pure
 * producer of the audit log's {@code changes}. Fed two {@code auditSnapshot()}
 * maps (one taken before a mutation, one after), it emits a {@link FieldChange}
 * for every field whose value actually changed.
 *
 * <p>Pure and deterministic: null-safe on both sides ({@code Objects.equals}),
 * changed-only (an unchanged field emits nothing), and stable order — the
 * union of the two maps' keys in first-seen order (before's keys, then any
 * after-only keys). No infrastructure, no Jackson; values pass straight
 * through as the snapshots produced them.
 */
public final class AuditChanges {

    private AuditChanges() {}

    public static List<FieldChange> diff(Map<String, Object> before, Map<String, Object> after) {
        Set<String> fields = new LinkedHashSet<>(before.keySet());
        fields.addAll(after.keySet());

        List<FieldChange> changes = new ArrayList<>();
        for (String field : fields) {
            Object from = before.get(field);
            Object to = after.get(field);
            if (!Objects.equals(from, to)) {
                changes.add(new FieldChange(field, from, to));
            }
        }
        return changes;
    }
}
