package com.vointika.touroperator.application.usecase;

import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.StorefrontPasswordView;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.UUID;

/** The operator's storefront password-protection settings. Any member. */
public class GetStorefrontPasswordUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetStorefrontPasswordUseCase(TourOperatorRepository tourOperatorRepository,
                                        TourOperatorMembershipCheck membershipCheck) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.membershipCheck = membershipCheck;
    }

    public StorefrontPasswordView execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        TourOperator operator = tourOperatorRepository.requireById(tourOperatorId);
        return new StorefrontPasswordView(
                operator.isPasswordEnabled(),
                operator.getStorefrontPassword(),
                operator.getPasswordMessage());
    }
}
