package com.vointika.audience.application.usecase;

import com.vointika.audience.domain.entity.AudienceTranslation;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.repository.AudienceTranslationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.UUID;

/**
 * Reads one locale's translation overlay. Any member. An untranslated locale
 * returns an empty overlay (name null) so the admin editor always has a form.
 */
public class GetAudienceTranslationUseCase {

    private final AudienceRepository audienceRepository;
    private final AudienceTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetAudienceTranslationUseCase(AudienceRepository audienceRepository,
                                         AudienceTranslationRepository translationRepository,
                                         TourOperatorMembershipCheck membershipCheck) {
        this.audienceRepository = audienceRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public AudienceTranslation execute(UUID tourOperatorId, UUID audienceId,
                                       String rawLocale, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        if (audienceRepository.findByIdAndTourOperatorId(audienceId, tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Audience not found");
        }
        LocaleCode locale = new LocaleCode(rawLocale);
        return translationRepository.findByAudienceIdAndLocale(audienceId, locale.value())
                .orElseGet(() -> AudienceTranslation.empty(audienceId, tourOperatorId, locale));
    }
}
