package com.vointika.metafield.application.usecase;

import com.vointika.metafield.domain.entity.MetaobjectEntry;
import com.vointika.metafield.domain.repository.MetaobjectEntryRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;

import java.util.List;
import java.util.UUID;

/** Unpublishes an entry. ADMIN+ only; 409 if not published (entity guard). */
public class UnpublishMetaobjectEntryUseCase {

    private final MetaobjectEntryRepository entryRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UnpublishMetaobjectEntryUseCase(MetaobjectEntryRepository entryRepository,
                                           TourOperatorMembershipCheck membershipCheck,
                                           TransactionRunner transactionRunner,
                                           AuditTrailPort auditTrailPort) {
        this.entryRepository = entryRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID entryId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        MetaobjectEntry entry = entryRepository.requireByIdAndTourOperatorId(entryId, tourOperatorId);
        entry.unpublish();
        transactionRunner.run(() -> {
            entryRepository.save(entry);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "METAOBJECT", entryId, "metaobject.unpublished", null,
                    List.of(new FieldChange("published", true, false))));
        });
    }
}
