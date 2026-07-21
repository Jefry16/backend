package com.vointika.touroperator.application.usecase;

import com.vointika.reference.domain.repository.LanguageRepository;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Sets an operator's content languages — its primary/default locale + the
 * supported set. ADMIN+ only; membership enforced by the interceptor.
 *
 * <p>Guards: caller not ADMIN+ → 403; a bad-shape or non-master-list locale
 * → 422 (validated against {@code reference.languages} via
 * {@link LanguageRepository#existsByCode}, the only gate — no FK); empty
 * supported set or primary ∉ supported → 422 (domain invariant); operator
 * missing → 404.
 */
public class UpdateOperatorLocalesUseCase {

    private final TourOperatorRepository tourOperatorRepository;
    private final LanguageRepository languageRepository;
    private final TourOperatorMembershipCheck membershipCheck;

    public UpdateOperatorLocalesUseCase(TourOperatorRepository tourOperatorRepository,
                                        LanguageRepository languageRepository,
                                        TourOperatorMembershipCheck membershipCheck) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.languageRepository = languageRepository;
        this.membershipCheck = membershipCheck;
    }

    public void execute(UUID tourOperatorId, String rawPrimary, List<String> rawSupported, UUID callerUserId) {
        membershipCheck.ensureAdmin(callerUserId, tourOperatorId);

        LocaleCode primary = new LocaleCode(rawPrimary);
        if (rawSupported == null || rawSupported.isEmpty()) {
            throw new InvalidFieldException("At least one supported locale is required");
        }
        Set<LocaleCode> supported = new LinkedHashSet<>();
        for (String raw : rawSupported) {
            LocaleCode code = new LocaleCode(raw);
            if (!languageRepository.existsByCode(code.value())) {
                throw new InvalidFieldException("Unsupported language code: " + code.value());
            }
            supported.add(code);
        }

        TourOperator operator = tourOperatorRepository.findById(tourOperatorId)
                .orElseThrow(() -> new ResourceNotFoundException("Tour operator not found"));
        // Domain enforces non-empty + primary ∈ supported. Since every supported
        // code is master-list-validated above and primary must be one of them,
        // the primary is validated transitively.
        operator.updateLocales(primary, supported);
        tourOperatorRepository.save(operator);
    }
}
