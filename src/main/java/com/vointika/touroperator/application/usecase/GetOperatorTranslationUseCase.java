package com.vointika.touroperator.application.usecase;

import com.vointika.touroperator.application.dto.output.OperatorTranslationView;
import com.vointika.touroperator.domain.entity.TourOperatorTranslation;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.repository.TourOperatorTranslationRepository;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.UUID;

/**
 * Reads one locale's operator overlay. Any member may view. An untranslated
 * locale returns an empty overlay (all fields null) so the admin editor always
 * has a form.
 */
public class GetOperatorTranslationUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final TourOperatorTranslationRepository translationRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public GetOperatorTranslationUseCase(TourOperatorRepository tourOperatorRepository,
                                         TourOperatorTranslationRepository translationRepository,
                                         TourOperatorMembershipCheck membershipCheck) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.translationRepository = translationRepository;
        this.membershipCheck = membershipCheck;
    }

    public OperatorTranslationView execute(UUID tourOperatorId, String rawLocale, UUID callerUserId) {
        membershipCheck.ensureMember(callerUserId, tourOperatorId);
        tourOperatorRepository.requireById(tourOperatorId);
        LocaleCode locale = new LocaleCode(rawLocale);
        TourOperatorTranslation translation = translationRepository
                .findByTourOperatorIdAndLocale(tourOperatorId, locale.value())
                .orElseGet(() -> TourOperatorTranslation.empty(tourOperatorId, locale));
        return OperatorTranslationView.from(translation);
    }
}
