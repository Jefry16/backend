package com.vointika.experience.application.usecase;

import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Publishes an experience (DRAFT → PUBLISHED). ADMIN+ only. 403 non-admin; 404
 * if not under this operator; 409 if already published.
 */
public class PublishExperienceUseCase {

    private final ExperienceRepository experienceRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public PublishExperienceUseCase(ExperienceRepository experienceRepository,
                                    TourOperatorMembershipCheck membershipCheck) {
        this.experienceRepository = experienceRepository;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID experienceId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Experience experience = experienceRepository.findByIdAndTourOperatorId(experienceId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));
        experience.publish();
        experienceRepository.save(experience);
    }
}
