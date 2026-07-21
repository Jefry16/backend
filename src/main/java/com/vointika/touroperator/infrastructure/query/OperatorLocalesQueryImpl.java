package com.vointika.touroperator.infrastructure.query;

import com.vointika.shared.port.OperatorLocalesQuery;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * touroperator's adapter for the shared {@link OperatorLocalesQuery} seam:
 * exposes the operator's supported content-locale codes to other contexts
 * (experience translations validate against this).
 */
@Component
public class OperatorLocalesQueryImpl implements OperatorLocalesQuery {

    private final TourOperatorRepository tourOperatorRepository;

    public OperatorLocalesQueryImpl(TourOperatorRepository tourOperatorRepository) {
        this.tourOperatorRepository = tourOperatorRepository;
    }

    @Override
    public Set<String> findSupportedLocales(UUID tourOperatorId) {
        return tourOperatorRepository.findById(tourOperatorId)
                .map(op -> op.getSupportedLocales().stream()
                        .map(LocaleCode::value)
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }
}
