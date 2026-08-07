package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.input.UpsertPolicyTranslationInput;
import com.vointika.touroperator.domain.entity.PolicyTranslation;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyRepository;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyTranslationRepository;
import com.vointika.touroperator.domain.valueobject.PolicyBody;
import com.vointika.touroperator.domain.valueobject.PolicyTitle;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.port.NewAuditEntry;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.valueobject.AuditActor;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.Map;
import java.util.UUID;

/**
 * Creates or replaces one policy's overlay for one locale. ADMIN+.
 *
 * <p>Guards: caller not ADMIN+ → 403; unknown type or unwritten policy → 404
 * (there is nothing to overlay); a locale outside the operator's supported set →
 * 422 ({@link OperatorLocalesQuery}). A blank field is treated as untranslated
 * (null → falls back to the canonical policy).
 */
public class UpsertPolicyTranslationUseCase {

    private final TourOperatorPolicyRepository policyRepository;
    private final TourOperatorPolicyTranslationRepository translationRepository;
    private final OperatorLocalesQuery operatorLocalesQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertPolicyTranslationUseCase(TourOperatorPolicyRepository policyRepository,
                                          TourOperatorPolicyTranslationRepository translationRepository,
                                          OperatorLocalesQuery operatorLocalesQuery,
                                          TourOperatorMembershipCheck membershipCheck,
                                          TransactionRunner transactionRunner,
                                          AuditTrailPort auditTrailPort) {
        this.policyRepository = policyRepository;
        this.translationRepository = translationRepository;
        this.operatorLocalesQuery = operatorLocalesQuery;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, String rawType, String rawLocale,
                        UpsertPolicyTranslationInput input, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        PolicyType type = PolicyType.from(rawType)
                .orElseThrow(() -> new ResourceNotFoundException("Policy not found"));
        LocaleCode locale = new LocaleCode(rawLocale);

        // The policy is the owner: translating one that was never written would
        // leave a row the storefront can never reach, since the canonical row is
        // what the page is looked up by.
        if (policyRepository.findByTourOperatorIdAndType(tourOperatorId, type).isEmpty()) {
            throw new ResourceNotFoundException("Policy not found");
        }
        if (!operatorLocalesQuery.findSupportedLocales(tourOperatorId).contains(locale.value())) {
            throw new InvalidFieldException(
                    "Locale '" + locale.value() + "' is not supported by this operator");
        }

        PolicyTitle title = blankNull(input.title()) == null
                ? null : new PolicyTitle(input.title());
        PolicyBody body = blankNull(input.body()) == null
                ? null : new PolicyBody(input.body());

        transactionRunner.run(() -> {
            translationRepository.upsert(
                    new PolicyTranslation(tourOperatorId, type, locale, title, body));
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "TOUR_OPERATOR", tourOperatorId, "tour_operator.policy_translation_updated",
                    Map.of("type", type.name(), "locale", locale.value())));
        });
    }

    private static String blankNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
