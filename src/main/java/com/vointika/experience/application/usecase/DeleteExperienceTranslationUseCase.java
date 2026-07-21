package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;

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

    public DeleteExperienceTranslationUseCase(ExperienceRepository experienceRepository,
                                              ExperienceTranslationRepository translationRepository,
                                              TourOperatorMembershipCheck membershipCheck) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID experienceId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        if (experienceRepository.findByIdAndTourOperatorId(experienceId, tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Experience not found");
        }
        translationRepository.deleteByExperienceIdAndLocale(experienceId, new LocaleCode(rawLocale).value());
    }
}
