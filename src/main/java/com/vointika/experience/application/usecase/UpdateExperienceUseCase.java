package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.input.ExperienceInput;
import com.vointika.experience.application.service.ExperienceInputMapper;
import com.vointika.experience.application.service.MediaReferenceValidator;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.exception.ResourceNotFoundException;

import java.util.UUID;

/**
 * Updates an experience's editable fields (everything but slug/status). ADMIN+
 * only. Guards: caller not ADMIN+ → 403; id not under this operator → 404;
 * invalid field or media ref → 422. The slug is immutable and the status is
 * changed only via publish/unpublish.
 */
public class UpdateExperienceUseCase {

    private final ExperienceRepository experienceRepository;
    private final MediaReferenceValidator mediaReferenceValidator;
    private final TourOperatorMembershipCheck membershipCheck;

    public UpdateExperienceUseCase(ExperienceRepository experienceRepository,
                                   MediaReferenceValidator mediaReferenceValidator,
                                   TourOperatorMembershipCheck membershipCheck) {
        this.experienceRepository = experienceRepository;
        this.mediaReferenceValidator = mediaReferenceValidator;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID experienceId, UUID callerUserId, ExperienceInput input) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Experience experience = experienceRepository.findByIdAndTourOperatorId(experienceId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));

        var mediaIds = ExperienceInputMapper.mediaIds(input);
        mediaReferenceValidator.validate(tourOperatorId, mediaIds, input.thumbnailMediaId());

        experience.update(
                ExperienceInputMapper.name(input),
                ExperienceInputMapper.description(input),
                ExperienceInputMapper.longDescription(input),
                input.featured(),
                ExperienceInputMapper.tags(input),
                ExperienceInputMapper.included(input),
                ExperienceInputMapper.notIncluded(input),
                ExperienceInputMapper.highlights(input),
                mediaIds,
                input.thumbnailMediaId(),
                ExperienceInputMapper.durationMinutes(input),
                ExperienceInputMapper.bookingCutoffHours(input));

        experienceRepository.save(experience);
    }
}
