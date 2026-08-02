package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.output.OperatorTranslationView;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.repository.TourOperatorTranslationRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;

import java.util.List;
import java.util.UUID;

/**
 * Lists the operator's translation overlays — one row per translated locale.
 * Any member may view. Bounded by the operator's supported locales, so a plain
 * list rather than the cursor framework (the same exemption the reference lists
 * have).
 */
public class ListOperatorTranslationsUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public ListOperatorTranslationsUseCase(TourOperatorRepository tourOperatorRepository,
                                           TourOperatorTranslationRepository translationRepository,
                                           TourOperatorMembershipCheck membershipCheck) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public List<OperatorTranslationView> execute(UUID tourOperatorId, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        if (tourOperatorRepository.findById(tourOperatorId).isEmpty()) {
            throw new ResourceNotFoundException("Tour operator not found");
        }
        return translationRepository.findAllByTourOperatorId(tourOperatorId).stream()
                .map(OperatorTranslationView::from)
                .toList();
    }
}
