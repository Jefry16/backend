package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.MediaKeyBatchQuery;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.Set;
import java.util.UUID;

/**
 * Sets the operator's logo to one of its own media records. ADMIN+ only;
 * membership enforced by the interceptor.
 *
 * <p>The media id is validated against the operator's library through the
 * {@link MediaKeyBatchQuery} seam — an id that isn't owned by this operator
 * (foreign, or nonexistent) is rejected (422), which is what keeps a logo from
 * pointing at another tenant's media without a DB foreign key. Guards: caller
 * not ADMIN+ → 403; unknown media → 422; operator missing → 404 (defensive).
 */
public class SetOperatorLogoUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final MediaKeyBatchQuery mediaKeyBatchQuery;
    private final TourOperatorMembershipCheck membershipCheck;

    public SetOperatorLogoUseCase(TourOperatorRepository tourOperatorRepository,
                                  MediaKeyBatchQuery mediaKeyBatchQuery,
                                  TourOperatorMembershipCheck membershipCheck) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.mediaKeyBatchQuery = mediaKeyBatchQuery;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID mediaId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        if (mediaId == null) {
            throw new InvalidFieldException("mediaId is required");
        }
        boolean ownedByOperator = mediaKeyBatchQuery
                .findKeysByIds(tourOperatorId, Set.of(mediaId))
                .containsKey(mediaId);
        if (!ownedByOperator) {
            throw new InvalidFieldException("Media not found in this operator's library");
        }

        TourOperator operator = tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));
        operator.setLogo(mediaId);
        tourOperatorRepository.save(operator);
    }
}
