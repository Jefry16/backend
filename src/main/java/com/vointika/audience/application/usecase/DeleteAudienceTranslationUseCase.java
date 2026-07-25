package com.vointika.audience.application.usecase;

import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.repository.AudienceTranslationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.UUID;

/** Removes one locale's translation overlay. ADMIN+. Idempotent. */
public class DeleteAudienceTranslationUseCase {

    private final AudienceRepository audienceRepository;
    private final AudienceTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public DeleteAudienceTranslationUseCase(AudienceRepository audienceRepository,
                                            AudienceTranslationRepository translationRepository,
                                            TourOperatorMembershipCheck membershipCheck) {
        this.audienceRepository = audienceRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID audienceId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        if (audienceRepository.findByIdAndTourOperatorId(audienceId, tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Audience not found");
        }
        translationRepository.deleteByAudienceIdAndLocale(audienceId, new LocaleCode(rawLocale).value());
    }
}
