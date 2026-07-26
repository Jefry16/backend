package com.vointika.audit.domain.repository;

import com.vointika.audit.domain.entity.AuditLogEntry;
import com.vointika.audit.domain.projection.AuditLogListItem;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

/**
 * Append + read only — the trail is append-only by construction, so no update
 * or delete surface exists anywhere in the context.
 */
public interface AuditLogEntryRepository {

    void append(AuditLogEntry entry);

    CursorPage<AuditLogListItem> list(ListQuery query);

    Optional<AuditLogListItem> findByIdAndTourOperatorId(UUID entryId, UUID tourOperatorId);
}
