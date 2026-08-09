package com.vointika.audience.application.usecase;

import com.vointika.audience.domain.entity.AudienceTranslation;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.repository.AudienceTranslationRepository;
import com.vointika.audience.domain.valueobject.AudienceName;
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
import java.util.Objects;
import java.util.UUID;

/**
 * Creates or replaces the translation overlay for one (audience, locale). ADMIN+.
 * Guards: caller not ADMIN+ → 403; audience not under this operator → 404;
 * bad-shape or unsupported locale → 422 (must be in the operator's supported
 * set).
 *
 * <p><b>A blank name deletes the row rather than storing an absent one.</b> Name
 * is the only translatable column, so a row without it falls back for
 * everything — indistinguishable from no row, except in the translations list,
 * where it appears as a locale someone has worked on. Blanking an already-absent
 * name is a no-op: nothing written, nothing audited.
 */
public class UpsertAudienceTranslationUseCase {

    private final AudienceRepository audienceRepository;
    private final AudienceTranslationRepository translationRepository;
    private final OperatorLocalesQuery operatorLocalesQuery;
    private final TourOperatorMembershipCheck membershipCheck;
    private final TransactionRunner transactionRunner;
    private final AuditTrailPort auditTrailPort;

    public UpsertAudienceTranslationUseCase(AudienceRepository audienceRepository,
                                            AudienceTranslationRepository translationRepository,
                                            OperatorLocalesQuery operatorLocalesQuery,
                                            TourOperatorMembershipCheck membershipCheck,
                                            TransactionRunner transactionRunner,
                                            AuditTrailPort auditTrailPort) {
        this.audienceRepository = audienceRepository;
        this.translationRepository = translationRepository;
        this.operatorLocalesQuery = operatorLocalesQuery;
        this.membershipCheck = membershipCheck;
        this.transactionRunner = transactionRunner;
        this.auditTrailPort = auditTrailPort;
    }

    public void execute(UUID tourOperatorId, UUID audienceId, String rawLocale,
                        String name, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);

        if (audienceRepository.findByIdAndTourOperatorId(audienceId, tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Audience not found");
        }
        if (!operatorLocalesQuery.findSupportedLocales(tourOperatorId).contains(locale.value())) {
            throw new InvalidFieldException("Locale '" + locale.value() + "' is not supported by this operator");
        }

        AudienceName translated = (name == null || name.isBlank()) ? null : new AudienceName(name);

        // A same-value re-save mutates nothing — no write, no audit entry.
        String current = translationRepository.findByAudienceIdAndLocale(audienceId, locale.value())
                .map(t -> t.name() == null ? null : t.name().value())
                .orElse(null);
        if (Objects.equals(current, translated == null ? null : translated.value())) {
            return;
        }

        AudienceTranslation translation =
                new AudienceTranslation(audienceId, tourOperatorId, locale, translated);

        // Reached only when a row existed with a name — the same-value check above
        // already returned for the nothing-to-nothing case.
        if (translation.isEmpty()) {
            transactionRunner.run(() -> {
                translationRepository.deleteByAudienceIdAndLocale(audienceId, locale.value());
                auditTrailPort.append(new NewAuditEntry(
                        tourOperatorId, AuditActor.user(callerUserId),
                        "AUDIENCE", audienceId, "audience.translation_deleted",
                        Map.of("locale", locale.value())));
            });
            return;
        }

        transactionRunner.run(() -> {
            translationRepository.upsert(translation);
            auditTrailPort.append(new NewAuditEntry(
                    tourOperatorId, AuditActor.user(callerUserId),
                    "AUDIENCE", audienceId, "audience.translation_updated",
                    Map.of("locale", locale.value())));
        });
    }
}
