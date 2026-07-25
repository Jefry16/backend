package com.vointika.audience.application.usecase;

import com.vointika.audience.domain.entity.AudienceTranslation;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.audience.domain.repository.AudienceTranslationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.UUID;

/** Lists an audience's translation overlays — one row per translated locale. Any member. */
public class ListAudienceTranslationsUseCase {

    private final AudienceRepository audienceRepository;
    private final AudienceTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListAudienceTranslationsUseCase(AudienceRepository audienceRepository,
                                           AudienceTranslationRepository translationRepository,
                                           TourOperatorMembershipCheck membershipCheck) {
        this.audienceRepository = audienceRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public List<AudienceTranslation> execute(UUID tourOperatorId, UUID audienceId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        if (audienceRepository.findByIdAndTourOperatorId(audienceId, tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Audience not found");
        }
        return translationRepository.findAllByAudienceId(audienceId);
    }
}
