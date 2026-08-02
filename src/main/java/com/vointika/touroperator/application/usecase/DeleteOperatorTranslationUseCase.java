package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.repository.TourOperatorTranslationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Map;
import java.util.UUID;

/**
 * Removes one locale's operator overlay. ADMIN+. Idempotent — deleting a missing
 * overlay is a no-op success that records nothing.
 */
public class DeleteOperatorTranslationUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteOperatorTranslationUseCase(TourOperatorRepository tourOperatorRepository,
                                            TourOperatorTranslationRepository translationRepository,
                                            TourOperatorMembershipCheck membershipCheck,
                                            TransactionRunner transactionRunner,
                                            AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        if (tourOperatorRepository.findById(tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Tour operator not found");
        }
        String locale = new LocaleCode(rawLocale).value();
        // Probe first: an idempotent delete that removes nothing records nothing.
        if (translationRepository.findByTourOperatorIdAndLocale(tourOperatorId, locale).isEmpty()) {
            return;
        }
        transactionRunner.run(() -> {
            translationRepository.deleteByTourOperatorIdAndLocale(tourOperatorId, locale);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.translation_deleted",
                    Map.of("locale", locale)));
        });
    }
}
