package com.vointika.audience.application.usecase;

import com.vointika.audience.domain.entity.AudienceTranslation;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.repository.AudienceTranslationRepository;
import com.vointika.audience.domain.valueobject.AudienceName;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.UUID;

/**
 * Creates or replaces the translation overlay for one (audience, locale). ADMIN+.
 * Guards: caller not ADMIN+ → 403; audience not under this operator → 404;
 * bad-shape or unsupported locale → 422 (must be in the operator's supported
 * set). A blank name is stored as absent → falls back to canonical.
 */
public class UpsertAudienceTranslationUseCase {

    private final AudienceRepository audienceRepository;
    private final AudienceTranslationRepository translationRepository;
    private final OperatorLocalesQuery operatorLocalesQuery;
    private final TourOperatorMembershipCheck membershipCheck;

    public UpsertAudienceTranslationUseCase(AudienceRepository audienceRepository,
                                            AudienceTranslationRepository translationRepository,
                                            OperatorLocalesQuery operatorLocalesQuery,
                                            TourOperatorMembershipCheck membershipCheck) {
        this.audienceRepository = audienceRepository;
        this.translationRepository = translationRepository;
        this.operatorLocalesQuery = operatorLocalesQuery;
        this.membershipCheck = membershipCheck;
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
        translationRepository.upsert(
                new AudienceTranslation(audienceId, tourOperatorId, locale, translated));
    }
}
