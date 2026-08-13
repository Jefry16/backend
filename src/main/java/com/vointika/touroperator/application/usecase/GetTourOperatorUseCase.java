package com.vointika.touroperator.application.usecase;

import com.vointika.reference.domain.entity.Country;
import com.vointika.reference.domain.repository.CountryRepository;
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
    private final CountryRepository countryRepository;

    public GetTourOperatorUseCase(TourOperatorRepository tourOperatorRepository,
                                  TourOperatorMembershipCheck membershipCheck,
                                  CountryRepository countryRepository) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.membershipCheck = membershipCheck;
        this.countryRepository = countryRepository;
    }

    public TourOperatorView execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        return tourOperatorRepository.findById(tourOperatorId)
                .map(operator -> TourOperatorView.from(operator, country(operator)))
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));
    }

    /** Null when the operator has no address yet, which is every operator that predates V15. */
    private Country country(com.vointika.touroperator.domain.entity.TourOperator operator) {
        if (operator.getAddress() == null) {
            return null;
        }
        return countryRepository.findById(operator.getAddress().countryId()).orElse(null);
    }
}
