package com.vointika.audience.application.usecase;

import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Reads one audience. Any member may view; non-member → 404; an id not under this
 * operator → 404.
 */
public class GetAudienceUseCase {

    private final AudienceRepository audienceRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetAudienceUseCase(AudienceRepository audienceRepository,
                              TourOperatorMembershipCheck membershipCheck) {
        this.audienceRepository = audienceRepository;
        this.membershipCheck = membershipCheck;
    }

    public Audience execute(UUID tourOperatorId, UUID audienceId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return audienceRepository.requireByIdAndTourOperatorId(audienceId, tourOperatorId);
    }
}
