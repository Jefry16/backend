package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.repository.MetafieldValueRepository;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.util.Map;
import java.util.UUID;

/** Deletes an entry (its values cascade). ADMIN+ only. */
public class DeleteMetaobjectEntryUseCase {

    private final MetaobjectEntryRepository entryRepository;
    private final MetafieldValueRepository metafieldValueRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteMetaobjectEntryUseCase(MetaobjectEntryRepository entryRepository,
                                        MetafieldValueRepository metafieldValueRepository,
                                        TourOperatorMembershipCheck membershipCheck,
                                        TransactionRunner transactionRunner,
                                        AuditTrailPort auditTrailPort) {
        this.entryRepository = entryRepository;
        this.metafieldValueRepository = metafieldValueRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID entryId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        MetaobjectEntry entry = entryRepository.requireByIdAndTourOperatorId(entryId, tourOperatorId);
        // A delete leaves no row to diff — identity rides details.
        transactionRunner.run(() -> {
            // Any metaobject_reference metafield pointing here would go
            // dangling — clear those values in the same tx.
            metafieldValueRepository.deleteReferencesTo(entryId);
            entryRepository.delete(entryId);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "METAOBJECT", entryId, "metaobject.deleted",
                    Map.of("handle", entry.getHandle().value(),
                            "name", entry.getName().value())));
        });
    }
}
