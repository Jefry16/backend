package com.vointika.page.application.usecase;

import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;

import java.util.Map;
import java.util.UUID;

/**
 * Deletes a page (translations cascade). ADMIN+ only; 404 if not under this
 * operator. The audit entry's details record WHAT was deleted — after the row
 * is gone, the trail is the only place its identity survives.
 */
public class DeletePageUseCase {

    private final PageRepository pageRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeletePageUseCase(PageRepository pageRepository,
                             TourOperatorMembershipCheck membershipCheck,
                             TransactionRunner transactionRunner,
                             AuditTrailPort auditTrailPort) {
        this.pageRepository = pageRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID pageId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Page page = pageRepository.findByIdAndTourOperatorId(pageId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));
        transactionRunner.run(() -> {
            pageRepository.delete(pageId);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "PAGE", pageId, "page.deleted",
                    Map.of("title", page.getTitle().value(),
                            "handle", page.getHandle().value())));
        });
    }
}
