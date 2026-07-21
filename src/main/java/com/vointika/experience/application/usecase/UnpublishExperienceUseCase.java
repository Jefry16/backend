package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Unpublishes an experience (PUBLISHED → DRAFT) — the off-switch that pulls it
 * from the storefront. ADMIN+ only. 403 non-admin; 404 if not under this
 * operator; 409 if already a draft.
 */
public class UnpublishExperienceUseCase {

    private final ExperienceRepository experienceRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public UnpublishExperienceUseCase(ExperienceRepository experienceRepository,
                                      TourOperatorMembershipCheck membershipCheck) {
        this.experienceRepository = experienceRepository;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID experienceId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Experience experience = experienceRepository.findByIdAndTourOperatorId(experienceId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
        experience.unpublish();
        experienceRepository.save(experience);
    }
}
