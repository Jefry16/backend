package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.port.OperatorTimezoneQuery;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import org.springframework.stereotype.Component;

import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

/**
 * touroperator's adapter for the shared {@link OperatorTimezoneQuery} seam:
 * resolves the operator's timezone (its {@code timezone_id} → the reference
 * timezone's IANA name → {@link ZoneId}). Used by the experience context to
 * judge slot dates against the operator's local "today".
 */
@Component
public class OperatorTimezoneQueryImpl implements OperatorTimezoneQuery {

    private final TourOperatorRepository tourOperatorRepository;
    private final TimezoneRepository timezoneRepository;

    public OperatorTimezoneQueryImpl(TourOperatorRepository tourOperatorRepository,
                                     TimezoneRepository timezoneRepository) {
        this.tourOperatorRepository = tourOperatorRepository;
        this.timezoneRepository = timezoneRepository;
    }

    @Override
    public Optional<ZoneId> findZoneId(UUID tourOperatorId) {
        return tourOperatorRepository.findById(tourOperatorId)
                .map(op -> op.getTimezoneId())
                .flatMap(timezoneRepository::findById)
                .map(tz -> ZoneId.of(tz.getName()));
    }
}
