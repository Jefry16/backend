package com.vointika.experience.application.usecase;

import com.vointika.experience.application.dto.output.ExperienceView;
import com.vointika.experience.domain.entity.Experience;
import com.vointika.experience.domain.repository.ExperienceRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.media.MediaUrlBatchResolver;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Reads one experience. Any member may view; non-member → 404; an id not under
 * this operator → 404. Media ids are resolved to absolute URLs at read time.
 */
public class GetExperienceUseCase {

    private final ExperienceRepository experienceRepository;
    private final MediaUrlBatchResolver mediaUrlBatchResolver;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetExperienceUseCase(ExperienceRepository experienceRepository,
                                MediaUrlBatchResolver mediaUrlBatchResolver,
                                TourOperatorMembershipCheck membershipCheck) {
        this.experienceRepository = experienceRepository;
        this.mediaUrlBatchResolver = mediaUrlBatchResolver;
        this.membershipCheck = membershipCheck;
    }

    public ExperienceView execute(UUID tourOperatorId, UUID experienceId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        Experience experience = experienceRepository.findByIdAndTourOperatorId(experienceId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Experience not found"));

        List<UUID> ids = new ArrayList<>(experience.getMediaIds());
        if (experience.getThumbnailMediaId() != null) {
            ids.add(experience.getThumbnailMediaId());
        }
        return ExperienceView.from(experience, mediaUrlBatchResolver.resolve(tourOperatorId, ids));
    }
}
