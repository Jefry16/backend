package com.vointika.audit.application.usecase;

import com.vointika.audit.domain.projection.AuditLogListItem;
import com.vointika.audit.domain.repository.AuditLogEntryRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.ListSchema;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.AuditActorType;

import java.util.UUID;

/**
 * Lists a tour operator's audit-trail entries, newest first. Cursor-paginated,
 * tenant-scoped; any member may read (the trail shows nothing a member can't
 * already see on the entity pages). Filterable by {@code entityType}/
 * {@code entityId} (a single entity's timeline), {@code action},
 * {@code actorType}/{@code actorId}, {@code actorName} (frozen at write; a
 * text filter — not sortable, the column is nullable and the keyset cursor
 * needs non-null sort keys), and a {@code createdAt} range. Default {@code -id}: ids
 * are UUIDv7, so descending id IS the reverse-chronological timeline and keeps
 * cursor pagination stable.
 */
public class ListAuditLogUseCase {

    public static final ListSchema SCHEMA = ListSchema.builder()
            .tenantScoped()
            .set("entityType", String.class)
            .set("entityId", UUID.class)
            .set("action", String.class)
            .set("actorType", AuditActorType.class)
            .set("actorId", UUID.class)
            .text("actorName")
            // Both are null for SYSTEM actors, and actorName is null again for a
            // USER whose account no longer resolves to a name. Neither can be made
            // NOT NULL — there is no user behind a SYSTEM row to invent one from —
            // so the negating operators are refused on them instead.
            .nullable("actorId")
            .nullable("actorName")
            .instant("createdAt")
            .sortable("id")
            .sortable("createdAt")
            // actorName is deliberately NOT sortable: it is nullable (SYSTEM
            // rows, unresolvable accounts) and the keyset cursor cannot
            // paginate over null sort keys — filter-only.
            .defaultSort("-id")
            .build();

    private final AuditLogEntryRepository auditLogEntryRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListAuditLogUseCase(AuditLogEntryRepository auditLogEntryRepository,
                               TourOperatorMembershipCheck membershipCheck) {
        this.auditLogEntryRepository = auditLogEntryRepository;
        this.membershipCheck = membershipCheck;
    }

    public CursorPage<AuditLogListItem> execute(ListQuery query, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, query.tenantId());
        return auditLogEntryRepository.list(query);
    }
}
