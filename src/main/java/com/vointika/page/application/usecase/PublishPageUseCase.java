package com.vointika.page.application.usecase;

import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;

import java.util.List;
import java.util.UUID;

/** DRAFT → PUBLISHED. ADMIN+; already published → 409 (in the entity). */
public class PublishPageUseCase {

    private final PageRepository pageRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public PublishPageUseCase(PageRepository pageRepository,
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
        page.publish();
        // Reaching here means the flip is real (an idempotent re-publish 409s
        // in the entity), so the diff is always exactly this one field.
        transactionRunner.run(() -> {
            pageRepository.save(page);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "PAGE", pageId, "page.published", null,
                    List.of(new FieldChange("published", false, true))));
        });
    }
}
