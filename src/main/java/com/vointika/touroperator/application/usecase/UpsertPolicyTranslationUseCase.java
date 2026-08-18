package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.input.UpsertPolicyTranslationInput;
import com.vointika.touroperator.domain.entity.PolicyTranslation;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyTranslationRepository;
import com.vointika.touroperator.domain.valueobject.PolicyBody;
import com.vointika.touroperator.domain.valueobject.PolicyTitle;
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
 * Creates or replaces one policy's overlay for one locale. ADMIN+.
 *
 * <p>Guards: caller not ADMIN+ → 403; a policy id that is unknown <em>or belongs
 * to another operator</em> → 404, since the lookup is tenant-scoped; a locale
 * outside the operator's supported set → 422 ({@link com.vointika.shared.service.OperatorLocaleCheck}). A
 * blank field is treated as untranslated (null → falls back to the canonical
 * policy).
 *
 * <p><b>Blanking every field deletes the row rather than storing one.</b> An
 * overlay with nothing in it falls back for every field, so it is
 * indistinguishable from no overlay — except in the translations list, where it
 * appears as a locale someone has worked on. Saving an already-blank form is a
 * no-op: nothing written, nothing audited.
 */
public class UpsertPolicyTranslationUseCase {

    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorPolicyTranslationRepository translationRepository;
    private final OperatorLocaleCheck operatorLocaleCheck;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertPolicyTranslationUseCase(TourOperatorPolicyRepository policyRepository,
                                          TourOperatorPolicyTranslationRepository translationRepository,
                                          OperatorLocaleCheck operatorLocaleCheck,
                                          TourOperatorMembershipCheck membershipCheck,
                                          TransactionRunner transactionRunner,
                                          AuditTrailPort auditTrailPort) {
        this.policyRepository = policyRepository;
        this.translationRepository = translationRepository;
        this.operatorLocaleCheck = operatorLocaleCheck;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID policyId, String rawLocale,
                        UpsertPolicyTranslationInput input, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);

        // The policy is the owner and the URL addresses it by id; its *type* is
        // what the overlay is stored under, since that is the pair the storefront
        // reads and the composite foreign key cascades on.
        PolicyType type = policyRepository.findByIdAndTourOperatorId(policyId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"))
                .type();
        operatorLocaleCheck.require(tourOperatorId, locale.value());

        PolicyTitle title = blankNull(input.title()) == null
                ? null : new PolicyTitle(input.title());
        PolicyBody body = blankNull(input.body()) == null
                ? null : new PolicyBody(input.body());
        PolicyTranslation translation =
                new PolicyTranslation(tourOperatorId, type, locale, title, body);

        if (translation.isEmpty()) {
            clear(tourOperatorId, type, locale, callerUserId);
            return;
        }

        transactionRunner.run(() -> {
            translationRepository.upsert(translation);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.policy_translation_updated",
                    Map.of("type", type.name(), "locale", locale.value())));
        });
    }

    /**
     * Blanking every field is how the editor says "this locale is not translated",
     * and the honest representation of that is no row. Audited as a delete
     * because that is what happened; a no-op when there was nothing to remove, so
     * saving an already-blank form writes nothing and logs nothing.
     */
    private void clear(UUID tourOperatorId, PolicyType type, LocaleCode locale, UUID callerUserId) {
        transactionRunner.run(() -> {
            if (!translationRepository.delete(tourOperatorId, type, locale.value())) {
                return;
            }
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.policy_translation_deleted",
                    Map.of("type", type.name(), "locale", locale.value())));
        });
    }

    private static String blankNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
