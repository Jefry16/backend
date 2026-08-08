package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.output.TourOperatorView;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/**
 * The operator's own details. Member-visible.
 *
 * <p>This closes a standing gap: {@code TourOperatorController} was create-only,
 * so nothing could read back the operator it had just made. The storefront
 * password, locales, SEO and brand each had their own read; the operator itself
 * had none.
 *
 * <p>The membership interceptor has already answered 404 for a non-member, so a
 * missing operator here means the row is genuinely gone.
 */
public class GetTourOperatorUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetTourOperatorUseCase(TourOperatorRepository tourOperatorRepository,
                                  TourOperatorMembershipCheck membershipCheck) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.membershipCheck = membershipCheck;
    }

    public TourOperatorView execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return tourOperatorRepository.findById(tourOperatorId)
                .map(TourOperatorView::from)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));
    }
}
