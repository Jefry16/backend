package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.UUID;

/**
 * Removes the operator's logo. ADMIN+ only; membership enforced by the
 * interceptor. Idempotent — clearing an already-empty logo is a no-op success.
 * Guards: caller not ADMIN+ → 403; operator missing → 404 (defensive).
 */
public class ClearOperatorLogoUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ClearOperatorLogoUseCase(TourOperatorRepository tourOperatorRepository,
                                    TourOperatorMembershipCheck membershipCheck) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);
        TourOperator operator = tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));
        operator.clearLogo();
        tourOperatorRepository.save(operator);
    }
}
