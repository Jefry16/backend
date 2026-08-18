package com.vointika.page.application.usecase;

import com.vointika.page.domain.repository.PageRepository;
import com.vointika.page.domain.repository.PageTranslationRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Map;
import java.util.UUID;

/** Removes one locale's overlay. ADMIN+. Probe-first idempotent: removing nothing records nothing. */
public class DeletePageTranslationUseCase {

    private final PageRepository pageRepository;
    private final PageTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeletePageTranslationUseCase(PageRepository pageRepository,
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

    public void execute(UUID tourOperatorId, UUID pageId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        pageRepository.requireExists(pageId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);
        if (translationRepository.find(pageId, locale).isEmpty()) {
            return;
        }
        transactionRunner.run(() -> {
            translationRepository.delete(pageId, locale);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "PAGE", pageId, "page.translation_deleted",
                    Map.of("locale", locale.value())));
        });
    }
}
