package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.output.ExperienceTranslationView;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.experience.domain.repository.ExperienceTranslationRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.UUID;

/**
 * Lists an experience's translation overlays — one row per translated locale.
 * Any member may view. Guards: non-member → 404; experience not under this
 * operator → 404.
 */
public class ListExperienceTranslationsUseCase {

    private final ExperienceRepository experienceRepository;
    private final ExperienceTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListExperienceTranslationsUseCase(ExperienceRepository experienceRepository,
                                             ExperienceTranslationRepository translationRepository,
                                             TourOperatorMembershipCheck membershipCheck) {
        this.experienceRepository = experienceRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public List<ExperienceTranslationView> execute(UUID tourOperatorId, UUID experienceId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        experienceRepository.requireExists(experienceId, tourOperatorId);
        return translationRepository.findAllByExperienceId(experienceId).stream()
                .map(ExperienceTranslationView::from)
                .toList();
    }
}
