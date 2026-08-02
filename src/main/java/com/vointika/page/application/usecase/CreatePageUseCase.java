package com.vointika.page.application.usecase;

import com.vointika.page.application.dto.input.CreatePageInput;
import com.vointika.page.domain.entity.Page;
import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.page.domain.valueobject.PageBody;
import com.vointika.page.domain.valueobject.PageSeoDescription;
import com.vointika.page.domain.valueobject.PageSeoTitle;
import com.vointika.page.domain.valueobject.PageTitle;
import com.vointika.shared.exception.ResourceAlreadyExistsException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.util.Map;
import java.util.UUID;

/**
 * Creates a page as DRAFT. ADMIN+ only. Unlike the experience canonical handle
 * (derived from the name with auto-suffix probing), the page handle is
 * OPERATOR-CHOSEN — it is the page's permanent URL, so a collision is a 409
 * the operator resolves, never a silent {@code -2} suffix. Double-guarded:
 * pre-check here, the DB unique index + DIVE catch as the concurrent backstop.
 */
public class CreatePageUseCase {

    private final PageRepository pageRepository;
    private final PageTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final IdGenerator idGenerator;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public CreatePageUseCase(PageRepository pageRepository,
                             PageTranslationRepository translationRepository,
                             TourOperatorMembershipCheck membershipCheck,
                             IdGenerator idGenerator,
                             TransactionRunner transactionRunner,
                             AuditTrailPort auditTrailPort) {
        this.pageRepository = pageRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
        this.idGenerator = idGenerator;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public UUID execute(CreatePageInput input) {
        membershipCheck.ensureAdmin(input.callerUserId(), input.tourOperatorId());

        PageTitle title = new PageTitle(input.title());
        Handle handle = new Handle(input.handle());
        PageBody body = new PageBody(input.body());
        PageSeoTitle seoTitle = input.seoTitle() == null || input.seoTitle().isBlank()
                ? null : new PageSeoTitle(input.seoTitle());
        PageSeoDescription seoDescription = input.seoDescription() == null || input.seoDescription().isBlank()
                ? null : new PageSeoDescription(input.seoDescription());

        if (pageRepository.existsByTourOperatorIdAndHandle(input.tourOperatorId(), handle.value())) {
            throw new ResourceAlreadyExistsException("A page with this handle already exists");
        }
        if (translationRepository.existsByHandleInAnyLocale(
                input.tourOperatorId(), handle.value(), null)) {
            throw new ResourceAlreadyExistsException(
                    "A page already uses this handle as a localized handle");
        }

        Page page = new Page(
                idGenerator.newId(), input.tourOperatorId(),
                title, handle, body, seoTitle, seoDescription,
                input.callerUserId());
        try {
            transactionRunner.run(() -> {
                pageRepository.save(page);
                auditTrailPort.append(new NewAuditEntry(
                        input.tourOperatorId(), AuditActor.user(input.callerUserId()),
                        "PAGE", page.getId(), "page.created",
                        Map.of("title", title.value(), "handle", handle.value())));
            });
        } catch (UniqueConstraintViolationException e) {
            throw new ResourceAlreadyExistsException("A page with this handle already exists");
        }
        return page.getId();
    }
}
