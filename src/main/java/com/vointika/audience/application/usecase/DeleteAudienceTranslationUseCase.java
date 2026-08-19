package com.vointika.audience.application.usecase;

import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.repository.AudienceTranslationRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Map;
import java.util.UUID;

/** Removes one locale's translation overlay. ADMIN+. Idempotent. */
public class DeleteAudienceTranslationUseCase {

    private final AudienceRepository audienceRepository;
    private final AudienceTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteAudienceTranslationUseCase(AudienceRepository audienceRepository,
                                            AudienceTranslationRepository translationRepository,
                                            TourOperatorMembershipCheck membershipCheck,
                                            TransactionRunner transactionRunner,
                                            AuditTrailPort auditTrailPort) {
        this.audienceRepository = audienceRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID audienceId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        audienceRepository.requireExists(audienceId, tourOperatorId);
        String locale = new LocaleCode(rawLocale).value();
        // Probe first: an idempotent delete that removes nothing records nothing.
        if (translationRepository.findByAudienceIdAndLocale(audienceId, locale).isEmpty()) {
            return;
        }
        transactionRunner.run(() -> {
            translationRepository.deleteByAudienceIdAndLocale(audienceId, locale);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "AUDIENCE", audienceId, "audience.translation_deleted",
                    Map.of("locale", locale)));
        });
    }
}
