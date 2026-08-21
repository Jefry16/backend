package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.CategoryTranslation;
import com.vointika.experience.domain.repository.CategoryRepository;
import com.vointika.experience.domain.repository.CategoryTranslationRepository;
import com.vointika.experience.domain.valueobject.CategoryName;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.OperatorLocaleCheck;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Creates or replaces the translation overlay for one (category, locale). ADMIN+.
 * Guards: caller not ADMIN+ → 403; category not under this operator → 404;
 * bad-shape or unsupported locale → 422 (must be in the operator's supported set).
 *
 * <p><b>A blank name deletes the row rather than storing an absent one.</b> Name
 * is the only translatable column, so a row without it falls back for everything
 * — indistinguishable from no row, except in the translations list, where it
 * appears as a locale someone has worked on. Blanking an already-absent name is a
 * no-op: nothing written, nothing audited.
 */
public class UpsertCategoryTranslationUseCase {

    private final CategoryRepository categoryRepository;
    private final CategoryTranslationRepository translationRepository;
    private final OperatorLocaleCheck operatorLocaleCheck;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertCategoryTranslationUseCase(CategoryRepository categoryRepository,
                                            CategoryTranslationRepository translationRepository,
                                            OperatorLocaleCheck operatorLocaleCheck,
                                            TourOperatorMembershipCheck membershipCheck,
                                            TransactionRunner transactionRunner,
                                            AuditTrailPort auditTrailPort) {
        this.categoryRepository = categoryRepository;
        this.translationRepository = translationRepository;
        this.operatorLocaleCheck = operatorLocaleCheck;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID categoryId, String rawLocale,
                        String name, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);

        categoryRepository.requireExists(categoryId, tourOperatorId);
        operatorLocaleCheck.require(tourOperatorId, locale.value());

        CategoryName translated = (name == null || name.isBlank()) ? null : new CategoryName(name);

        // A same-value re-save mutates nothing — no write, no audit entry.
        String current = translationRepository.findByCategoryIdAndLocale(categoryId, locale.value())
                .map(t -> t.name() == null ? null : t.name().value())
                .orElse(null);
        if (Objects.equals(current, translated == null ? null : translated.value())) {
            return;
        }

        CategoryTranslation translation =
                new CategoryTranslation(categoryId, tourOperatorId, locale, translated);

        // Reached only when a row existed with a name — the same-value check above
        // already returned for the nothing-to-nothing case.
        if (translation.isEmpty()) {
            transactionRunner.run(() -> {
                translationRepository.deleteByCategoryIdAndLocale(categoryId, locale.value());
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "CATEGORY", categoryId, "category.translation_deleted",
                        Map.of("locale", locale.value())));
            });
            return;
        }

        transactionRunner.run(() -> {
            translationRepository.upsert(translation);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "CATEGORY", categoryId, "category.translation_updated",
                    Map.of("locale", locale.value())));
        });
    }
}
