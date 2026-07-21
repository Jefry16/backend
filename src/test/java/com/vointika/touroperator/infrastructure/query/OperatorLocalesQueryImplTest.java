package com.vointika.touroperator.infrastructure.query;

import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.shared.valueobject.Slug;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperatorLocalesQueryImplTest {

    private TourOperatorRepository repository;
    private OperatorLocalesQueryImpl query;
    private final UUID operatorId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = mock(TourOperatorRepository.class);
        query = new OperatorLocalesQueryImpl(repository);
    }

    @Test
    void returnsTheOperatorsSupportedLocaleCodes() {
        TourOperator op = new TourOperator(operatorId, new TourOperatorName("Acme"), new Slug("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("1 St"), UUID.randomUUID());
        op.updateLocales(LocaleCode.of("en"),
                new LinkedHashSet<>(List.of(LocaleCode.of("en"), LocaleCode.of("es"))));
        when(repository.findById(operatorId)).thenReturn(Optional.of(op));

        assertEquals(Set.of("en", "es"), query.findSupportedLocales(operatorId));
    }

    @Test
    void emptyWhenOperatorAbsent() {
        when(repository.findById(operatorId)).thenReturn(Optional.empty());
        assertTrue(query.findSupportedLocales(operatorId).isEmpty());
    }
}
