package com.vointika.audit.infrastructure.integration;

import com.vointika.audit.domain.entity.AuditLogEntry;
import com.vointika.audit.domain.repository.AuditLogEntryRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActorType;
import com.vointika.shared.web.CorrelationIdFilter;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Adapter behind {@link AuditTrailPort}. Deliberately carries no
 * {@code @Transactional}: the insert must JOIN the caller's ambient
 * transaction so the entry commits and rolls back atomically with the action
 * it records — a REQUIRES_NEW here would break both guarantees.
 *
 * <p>Freezes the acting user's display name at write time (one identity
 * lookup inside the caller's transaction): the trail shows who did it as they
 * were known THEN, and the read side needs no identity join. Best-effort — an
 * unresolvable account stores a null name, never fails the append.
 */
@Component
public class AuditTrailPortImpl implements AuditTrailPort {

    private static final int REQUEST_ID_MAX = 64;

    private final AuditLogEntryRepository repository;
    private final UserAccountQuery userAccountQuery;
    private final IdGenerator idGenerator;

    public AuditTrailPortImpl(AuditLogEntryRepository repository,
                              UserAccountQuery userAccountQuery,
                              IdGenerator idGenerator) {
        this.repository = repository;
        this.userAccountQuery = userAccountQuery;
        this.idGenerator = idGenerator;
    }

    @Override
    public void append(NewAuditEntry entry) {
        repository.append(AuditLogEntry.record(
                idGenerator.newId(),
                entry.tourOperatorId(),
                entry.actor(),
                resolveActorName(entry),
                entry.entityType(),
                entry.entityId(),
                entry.action(),
                entry.details(),
                entry.changes(),
                currentRequestId(),
                Instant.now()
        ));
    }

    private String resolveActorName(NewAuditEntry entry) {
        if (entry.actor().type() != AuditActorType.USER) {
            return null;
        }
        UUID userId = entry.actor().id();
        return userAccountQuery.findAccounts(Set.of(userId)).stream()
                .filter(account -> userId.equals(account.userId()))
                .map(account -> account.name())
                .findFirst()
                .orElse(null);
    }

    /** Best-effort correlation with the request logs; null off-request (e.g. schedulers). */
    private static String currentRequestId() {
        String requestId = MDC.get(CorrelationIdFilter.REQUEST_ID_MDC_KEY);
        if (requestId == null) {
            return null;
        }
        return requestId.length() > REQUEST_ID_MAX ? requestId.substring(0, REQUEST_ID_MAX) : requestId;
    }
}
