package com.vointika.shared.valueobject;

/**
 * One field-level change in an audited mutation — the {@code from → to} of a
 * single domain field, the atom of the change history serialized into the
 * audit log's {@code changes} JSONB.
 *
 * <p>{@code field} is the domain field name (the UI maps it to a label).
 * {@code from} / {@code to} are JSON-native values (String, Number, Boolean, a
 * List of those, or {@code null}) — never a domain value object, so no context
 * type leaks into {@code shared}. A list-valued field snapshots as a
 * {@code List} of scalars and diffs whole-value: any reorder/add/remove is one
 * {@code from → to} of the full list. Enums pass as their {@code name()},
 * temporals as ISO strings; the producer (an entity's {@code auditSnapshot()})
 * owns that normalization so values compare and serialize deterministically.
 * Either side may be {@code null} (set from nothing / cleared to nothing).
 */
public record FieldChange(String field, Object from, Object to) {
}
