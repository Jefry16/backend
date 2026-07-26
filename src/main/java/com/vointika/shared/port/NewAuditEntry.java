package com.vointika.shared.port;

import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One audit-trail append, as seen by the writing context. The audit context
 * assigns the entry id and timestamp, freezes the acting user's display name,
 * and captures the request correlation id.
 *
 * <p>{@code entityType} is the caller-owned uppercase noun ({@code "EXPERIENCE"},
 * {@code "AUDIENCE"}, …); {@code action} is dot-namespaced, past tense
 * ({@code "experience.updated"}, {@code "slot.cancelled"}, …). Both stay
 * strings: a closed shared enum would enumerate context concepts in the shared
 * kernel and force a shared-module edit per adopting context.
 *
 * <p>Two payload channels, either optional:
 * <ul>
 *   <li>{@code details} — action metadata that is NOT a field change (a
 *       translation's locale, an invitation's email). JSON-native values, no
 *       nulls; {@code null} for none.</li>
 *   <li>{@code changes} — the field-level {@code from → to} diff (a
 *       {@link FieldChange} per changed field, produced by
 *       {@code AuditChanges.diff}). Empty for pure events with no field
 *       change.</li>
 * </ul>
 */
public record NewAuditEntry(
        UUID tourOperatorId,
        AuditActor actor,
        String entityType,
        UUID entityId,
        String action,
        Map<String, Object> details,
        List<FieldChange> changes) {

    /** Details-only entry (no field diff) — the pure-event shape. */
    public NewAuditEntry(UUID tourOperatorId, AuditActor actor, String entityType, UUID entityId,
                         String action, Map<String, Object> details) {
        this(tourOperatorId, actor, entityType, entityId, action, details, List.of());
    }
}
