package com.vointika.page.application.usecase;

import com.vointika.page.application.dto.input.UpdatePageInput;
import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.AuditChanges;
import com.vointika.shared.valueobject.FieldChange;
import com.vointika.shared.valueobject.Handle;

import java.util.List;
import java.util.Map;

/**
 * Whole replace of the editable content (title/body required; null/blank SEO
 * fields and template suffix clear them). ADMIN+ only. Handle and status stay
 * untouched — the handle changes through rename, status through
 * publish/unpublish. The template suffix is validated handle-shaped; whether the
 * variant template exists is a render-time concern (the theme arc), mirroring
 * experiences' dangling-suffix fallback.
 */
public class UpdatePageUseCase {

    private final PageRepository pageRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpdatePageUseCase(PageRepository pageRepository,
                             TourOperatorMembershipCheck membershipCheck,
                             TransactionRunner transactionRunner,
                             AuditTrailPort auditTrailPort) {
        this.pageRepository = pageRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UpdatePageInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());

        Page page = pageRepository.findByIdAndTourOperatorId(input.pageId(), input.tourOperatorId())
                .orElseThrow(() -> new ResourceNotFoundException("Page not found"));

        PageTitle title = new PageTitle(input.title());
        PageBody body = new PageBody(input.body());
        PageSeoTitle seoTitle = input.seoTitle() == null || input.seoTitle().isBlank()
                ? null : new PageSeoTitle(input.seoTitle());
        PageSeoDescription seoDescription = input.seoDescription() == null || input.seoDescription().isBlank()
                ? null : new PageSeoDescription(input.seoDescription());
        String templateSuffix = (input.templateSuffix() == null || input.templateSuffix().isBlank())
                ? null
                : new Handle(input.templateSuffix()).value();

        Map<String, Object> before = page.auditSnapshot();
        page.update(title, body, seoTitle, seoDescription, templateSuffix);
        // A no-op replace (nothing actually changed) saves but records nothing.
        List<FieldChange> changes = AuditChanges.diff(before, page.auditSnapshot());
        transactionRunner.run(() -> {
            pageRepository.save(page);
            if (!changes.isEmpty()) {
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "PAGE", page.getId(), "page.updated", null, changes));
            }
        });
    }
}
