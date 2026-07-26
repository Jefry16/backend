package com.vointika.shared.port;

/**
 * Appends entries to the platform-wide audit trail (owned by the {@code audit}
 * context). Any context that mutates an operator-facing entity records the
 * action through this port.
 *
 * <p><strong>Transactional contract:</strong> {@link #append} MUST be called
 * inside the caller's business transaction (the {@code TransactionRunner}
 * block that performs the mutation). The entry then commits and rolls back
 * atomically with the action it records. A failed append fails the action —
 * "no unaudited mutation" is the invariant: a mutation without its record is
 * worse than a rejected request. (A mutation whose target is object storage
 * rather than the DB appends in its own transaction AFTER the successful
 * write instead — storage can't roll back, so that is the honest best-effort.)
 */
public interface AuditTrailPort {

    void append(NewAuditEntry entry);
}
