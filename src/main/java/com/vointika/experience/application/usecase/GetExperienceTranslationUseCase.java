package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.output.ExperienceTranslationView;
import com.vointika.experience.domain.entity.ExperienceTranslation;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.UUID;

/**
 * Reads one locale's translation overlay. Any member may view. An untranslated
 * locale returns an empty overlay (all fields null) so the admin editor always
 * has a form. Guards: non-member → 404; experience not under this operator → 404.
 */
public class GetExperienceTranslationUseCase {

    private final ExperienceRepository experienceRepository;
    private final ExperienceTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetExperienceTranslationUseCase(ExperienceRepository experienceRepository,
                                           ExperienceTranslationRepository translationRepository,
                                           TourOperatorMembershipCheck membershipCheck) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public ExperienceTranslationView execute(UUID tourOperatorId, UUID experienceId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        experienceRepository.requireExists(experienceId, tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);
        ExperienceTranslation translation = translationRepository
                .findByExperienceIdAndLocale(experienceId, locale.value())
                .orElseGet(() -> ExperienceTranslation.empty(experienceId, tourOperatorId, locale));
        return ExperienceTranslationView.from(translation);
    }
}
