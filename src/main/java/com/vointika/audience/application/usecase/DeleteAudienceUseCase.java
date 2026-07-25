package com.vointika.audience.application.usecase;

import com.vointika.audience.domain.entity.Audience;
import com.vointika.audience.domain.repository.AudienceRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * Deletes an audience. ADMIN+ only. 404 if the id isn't under this operator.
 *
 * <p>Slots snapshot an audience's name + price when priced (frozen), and
 * reference it by a bare id (no FK), so deleting an audience never cascades into
 * existing slot pricing — the snapshot survives. It only removes the tier from
 * the catalog for future slots.
 */
public class DeleteAudienceUseCase {

    private final AudienceRepository audienceRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public DeleteAudienceUseCase(AudienceRepository audienceRepository,
                                 TourOperatorMembershipCheck membershipCheck) {
        this.audienceRepository = audienceRepository;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID audienceId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        Audience audience = audienceRepository.findByIdAndTourOperatorId(audienceId, tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Audience not found"));
        audienceRepository.deleteById(audience.getId());
    }
}
