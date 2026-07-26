package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
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
 * Removes one locale's translation overlay. ADMIN+. Idempotent — deleting a
 * missing overlay is a no-op success. Guards: caller not ADMIN+ → 403;
 * experience not under this operator → 404.
 */
public class DeleteExperienceTranslationUseCase {

    private final ExperienceRepository experienceRepository;
    private final ExperienceTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteExperienceTranslationUseCase(ExperienceRepository experienceRepository,
                                              ExperienceTranslationRepository translationRepository,
                                              TourOperatorMembershipCheck membershipCheck,
                                              TransactionRunner transactionRunner,
                                              AuditTrailPort auditTrailPort) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID experienceId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        if (experienceRepository.findByIdAndTourOperatorId(experienceId, tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Experience not found");
        }
        String locale = new LocaleCode(rawLocale).value();
        // Probe first: an idempotent delete that removes nothing records nothing.
        if (translationRepository.findByExperienceIdAndLocale(experienceId, locale).isEmpty()) {
            return;
        }
        transactionRunner.run(() -> {
            translationRepository.deleteByExperienceIdAndLocale(experienceId, locale);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "EXPERIENCE", experienceId, "experience.translation_deleted",
                    Map.of("locale", locale)));
        });
    }
}
