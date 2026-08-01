package com.vointika.page.application.usecase;

import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.valueobject.Slug;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.Map;
import java.util.UUID;

/**
 * Renames a page's canonical handle — its own endpoint (not part of the
 * content PATCH) because changing the permanent URL is a deliberate act.
 * ADMIN+ only. The new handle is Slug-validated (422) and must be free among
 * the operator's handles (409, no auto-suffix — double-guarded by the
 * pre-check and the DB unique index). Renaming to the current handle is a
 * no-op (no timestamp churn, no audit entry). Slug-history 301s for the old
 * URL are deferred to the storefront arc.
 */
public class RenamePageUseCase {

    private final PageRepository pageRepository;
    private final PageTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public RenamePageUseCase(PageRepository pageRepository,
                             PageTranslationRepository translationRepository,
                             TourOperatorMembershipCheck membershipCheck,
                             TransactionRunner transactionRunner,
                             AuditTrailPort auditTrailPort) {
        this.pageRepository = pageRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID pageId, String newHandle, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Page page = pageRepository.findByIdAndTourOperatorId(pageId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));

        Slug handle = new Slug(newHandle);
        if (page.getHandle().value().equals(handle.value())) {
            return;
        }
        if (pageRepository.existsByTourOperatorIdAndHandle(tourOperatorId, handle.value())) {
            throw new ResourceAlreadyExistsException("A page with this handle already exists");
        }
        if (translationRepository.existsBySlugInAnyLocale(
                tourOperatorId, handle.value(), pageId)) {
            throw new ResourceAlreadyExistsException(
                    "A page already uses this handle as a localized handle");
        }

        Map<String, Object> before = page.auditSnapshot();
        page.rename(handle);
        try {
            transactionRunner.run(() -> {
                pageRepository.save(page);
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "PAGE", pageId, "page.renamed", null,
                        AuditChanges.diff(before, page.auditSnapshot())));
            });
        } catch (UniqueConstraintViolationException e) {
            throw new ResourceAlreadyExistsException("A page with this handle already exists");
        }
    }
}
