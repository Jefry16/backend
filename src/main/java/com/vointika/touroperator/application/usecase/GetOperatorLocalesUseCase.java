package com.vointika.touroperator.application.usecase;

import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.application.dto.output.OperatorLocalesView;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.UUID;

/**
 * Reads an operator's content-language settings (primary + supported set).
 * Any member may view (read-only; changing them is ADMIN+). Membership is
 * enforced by the interceptor and re-asserted here; non-member → 404.
 */
public class GetOperatorLocalesUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetOperatorLocalesUseCase(TourOperatorRepository tourOperatorRepository,
                                     TourOperatorMembershipCheck membershipCheck) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.membershipCheck = membershipCheck;
    }

    public OperatorLocalesView execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        TourOperator operator = tourOperatorRepository.requireById(tourOperatorId);
        return OperatorLocalesView.from(operator);
    }
}
