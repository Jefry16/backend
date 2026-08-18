package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.input.UpsertOperatorTranslationInput;
import com.vointika.touroperator.domain.entity.TourOperatorTranslation;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.repository.TourOperatorTranslationRepository;
import com.vointika.touroperator.domain.valueobject.BrandShortDescription;
import com.vointika.touroperator.domain.valueobject.BrandSlogan;
import com.vointika.touroperator.domain.valueobject.OperatorSeoDescription;
import com.vointika.touroperator.domain.valueobject.OperatorSeoTitle;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.service.OperatorLocaleCheck;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Map;
import java.util.UUID;

/**
 * Creates or replaces the operator's own translation overlay for one locale —
 * the operator-level text every page type falls back to. ADMIN+; membership enforced
 * by the interceptor.
 *
 * <p>Guards: caller not ADMIN+ → 403; operator missing → 404; a locale outside
 * the operator's supported set → 422 ({@link OperatorLocalesQuery}). A blank
 * field is treated as untranslated (null → falls back to canonical).
 *
 * <p><b>Blanking every field deletes the row rather than storing one.</b> An
 * overlay with nothing in it falls back for every field, so it is
 * indistinguishable from no overlay — except in the translations list, where it
 * appears as a locale someone has worked on. Saving an already-blank form is a
 * no-op: nothing written, nothing audited.
 */
public class UpsertOperatorTranslationUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorTranslationRepository translationRepository;
    private final OperatorLocaleCheck operatorLocaleCheck;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertOperatorTranslationUseCase(TourOperatorRepository tourOperatorRepository,
                                            TourOperatorTranslationRepository translationRepository,
                                            OperatorLocaleCheck operatorLocaleCheck,
                                            TourOperatorMembershipCheck membershipCheck,
                                            TransactionRunner transactionRunner,
                                            AuditTrailPort auditTrailPort) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.translationRepository = translationRepository;
        this.operatorLocaleCheck = operatorLocaleCheck;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, String rawLocale,
                        UpsertOperatorTranslationInput input, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);

        if (tourOperatorRepository.findById(tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Tour operator not found");
        }
        operatorLocaleCheck.require(tourOperatorId, locale.value());

        OperatorSeoTitle seoTitle = blankNull(input.seoTitle()) == null
                ? null : new OperatorSeoTitle(input.seoTitle());
        OperatorSeoDescription seoDescription = blankNull(input.seoDescription()) == null
                ? null : new OperatorSeoDescription(input.seoDescription());
        String passwordMessage = blankNull(input.passwordMessage());
        BrandSlogan slogan = blankNull(input.slogan()) == null
                ? null : new BrandSlogan(input.slogan());
        BrandShortDescription shortDescription = blankNull(input.shortDescription()) == null
                ? null : new BrandShortDescription(input.shortDescription());

        TourOperatorTranslation translation = new TourOperatorTranslation(
                tourOperatorId, locale, seoTitle, seoDescription, passwordMessage,
                slogan, shortDescription);

        if (translation.isEmpty()) {
            clear(tourOperatorId, locale, callerUserId);
            return;
        }

        transactionRunner.run(() -> {
            translationRepository.upsert(translation);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.translation_updated",
                    Map.of("locale", locale.value())));
        });
    }

    /**
     * Blanking every field is how the editor says "this locale is not translated",
     * and the honest representation of that is no row. Audited as a delete
     * because that is what happened; a no-op when there was nothing to remove, so
     * saving an already-blank form writes nothing and logs nothing.
     */
    private void clear(UUID tourOperatorId, LocaleCode locale, UUID callerUserId) {
        transactionRunner.run(() -> {
            if (!translationRepository.deleteByTourOperatorIdAndLocale(tourOperatorId, locale.value())) {
                return;
            }
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.translation_deleted",
                    Map.of("locale", locale.value())));
        });
    }

    private static String blankNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
