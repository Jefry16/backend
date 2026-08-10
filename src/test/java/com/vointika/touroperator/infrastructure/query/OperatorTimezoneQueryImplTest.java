package com.vointika.touroperator.infrastructure.query;

import com.vointika.reference.domain.entity.Timezone;
import com.vointika.reference.domain.repository.TimezoneRepository;
import com.vointika.shared.valueobject.Handle;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The seam `experience` uses to judge a slot's date against the operator's local
 * "today" — so an empty answer here is not cosmetic, it decides whether a
 * departure is in the past.
 *
 * <p>Two hops, and each can come back empty: the operator may not exist, and its
 * {@code timezone_id} may not resolve to a reference row. Both must yield
 * {@link Optional#empty()} rather than throw, because the caller treats absence
 * as "cannot judge" and a `NoSuchElementException` here would surface as a 500
 * from a slot create.
 */
class OperatorTimezoneQueryImplTest {

    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID TZ = UUID.fromString("019f8200-0000-7000-8000-000000000001");
    private static final UUID CUR = UUID.fromString("019f8200-0000-7000-8000-000000000002");

    private TourOperatorRepository operatorRepository;
    private TimezoneRepository timezoneRepository;
    private OperatorTimezoneQueryImpl query;

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        timezoneRepository = mock(TimezoneRepository.class);
        query = new OperatorTimezoneQueryImpl(operatorRepository, timezoneRepository);
    }

    private TourOperator operator() {
        return new TourOperator(OP, new TourOperatorName("Acme Tours"), new Handle("acme"),
                TZ, CUR, new TourOperatorAddress("Calle Mayor 1"), UUID.randomUUID(),
                Instant.now(), Instant.now(), LocaleCode.of("es"), Set.of(LocaleCode.of("es")));
    }

    @Test
    void resolvesTheOperatorsZoneFromItsReferenceTimezone() {
        Timezone madrid = mock(Timezone.class);
        when(madrid.getName()).thenReturn("Europe/Madrid");
        when(operatorRepository.findById(OP)).thenReturn(Optional.of(operator()));
        when(timezoneRepository.findById(TZ)).thenReturn(Optional.of(madrid));

        assertThat(query.findZoneId(OP)).contains(ZoneId.of("Europe/Madrid"));
    }

    @Test
    void anUnknownOperatorIsEmptyAndNeverReachesTheReferenceTable() {
        when(operatorRepository.findById(OP)).thenReturn(Optional.empty());

        assertThat(query.findZoneId(OP)).isEmpty();
        verifyNoInteractions(timezoneRepository);
    }

    @Test
    void aTimezoneIdThatResolvesToNothingIsEmptyRatherThanAThrow() {
        when(operatorRepository.findById(OP)).thenReturn(Optional.of(operator()));
        when(timezoneRepository.findById(TZ)).thenReturn(Optional.empty());

        assertThat(query.findZoneId(OP)).isEmpty();
    }
}
