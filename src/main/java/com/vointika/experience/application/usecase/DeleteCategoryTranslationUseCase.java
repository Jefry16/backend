package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.experience.domain.repository.CategoryTranslationRepository;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Map;
import java.util.UUID;

/** Removes one locale's translation overlay. ADMIN+. Idempotent. */
public class DeleteCategoryTranslationUseCase {

    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public DeleteCategoryTranslationUseCase(CategoryRepository categoryRepository,
                                            CategoryTranslationRepository translationRepository,
                                            TourOperatorMembershipCheck membershipCheck,
                                            TransactionRunner transactionRunner,
                                            AuditTrailPort auditTrailPort) {
        this.categoryRepository = categoryRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID categoryId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        categoryRepository.requireExists(categoryId, tourOperatorId);
        String locale = new LocaleCode(rawLocale).value();
        // Probe first: an idempotent delete that removes nothing records nothing.
        if (translationRepository.findByCategoryIdAndLocale(categoryId, locale).isEmpty()) {
            return;
        }
        transactionRunner.run(() -> {
            translationRepository.deleteByCategoryIdAndLocale(categoryId, locale);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "CATEGORY", categoryId, "category.translation_deleted",
                    Map.of("locale", locale)));
        });
    }
}
