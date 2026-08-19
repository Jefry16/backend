package com.vointika.page.application.usecase;

import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.FieldChange;

import java.util.List;
import java.util.UUID;

/** PUBLISHED → DRAFT — pulls the page from the (future) storefront. ADMIN+; already a draft → 409. */
public class UnpublishPageUseCase {

    private final PageRepository pageRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UnpublishPageUseCase(PageRepository pageRepository,
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
        Page page = pageRepository
.requireByIdAndTourOperatorId(pageId, tourOperatorId);
        page.unpublish();
        transactionRunner.run(() -> {
            pageRepository.save(page);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "PAGE", pageId, "page.unpublished", null,
                    List.of(new FieldChange("published", true, false))));
        });
    }
}
