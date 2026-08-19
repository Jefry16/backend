package com.vointika.audit.application.usecase;

import com.vointika.audit.domain.projection.AuditLogListItem;
import com.vointika.audit.domain.repository.AuditLogEntryRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * A single audit entry by id (a list row's shape), scoped to the operator.
 * Any member; a missing or cross-tenant id is the byte-identical 404.
 */
public class GetAuditLogEntryUseCase {

    /**
     * The 404 for an entry id this operator does not own.
     *
     * <p>Thrown once, so this is not a duplication fix — it exists so the
     * documentation test can <b>derive</b> the body it publishes. It hardcoded the
     * sentence, which meant rewording the throw moved nothing in the guide: the
     * §9a defect where centralising the rule is not centralising the contract.
     */
    public static final String NOT_FOUND = "Audit log entry not found";

    private final AuditLogEntryRepository auditLogEntryRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetAuditLogEntryUseCase(AuditLogEntryRepository auditLogEntryRepository,
                                   TourOperatorMembershipCheck membershipCheck) {
        this.auditLogEntryRepository = auditLogEntryRepository;
        this.membershipCheck = membershipCheck;
    }

    public AuditLogListItem execute(UUID tourOperatorId, UUID entryId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return auditLogEntryRepository.findByIdAndTourOperatorId(entryId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException(NOT_FOUND));
    }
}
