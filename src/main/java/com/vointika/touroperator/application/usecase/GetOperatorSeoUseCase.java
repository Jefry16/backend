package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.output.OperatorSeoView;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.UUID;

/** The operator's canonical SEO defaults. Any member. */
public class GetOperatorSeoUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetOperatorSeoUseCase(TourOperatorRepository tourOperatorRepository,
                                 TourOperatorMembershipCheck membershipCheck) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.membershipCheck = membershipCheck;
    }

    public OperatorSeoView execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        TourOperator operator = tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));
        return new OperatorSeoView(
                operator.getSeoTitle() == null ? null : operator.getSeoTitle().value(),
                operator.getSeoDescription() == null ? null : operator.getSeoDescription().value(),
                operator.getOgImageMediaId());
    }
}
