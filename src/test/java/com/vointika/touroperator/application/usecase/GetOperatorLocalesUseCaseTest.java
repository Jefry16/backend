package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.touroperator.application.dto.output.OperatorLocalesView;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetOperatorLocalesUseCaseTest {

    private TourOperatorRepository operatorRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private GetOperatorLocalesUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new GetOperatorLocalesUseCase(operatorRepository, membershipCheck);
    }

    private TourOperator operator() {
        TourOperator op = new TourOperator(operatorId, new TourOperatorName("Acme"), new Slug("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("1 St"), UUID.randomUUID());
        op.updateLocales(LocaleCode.of("es"),
                new LinkedHashSet<>(List.of(LocaleCode.of("en"), LocaleCode.of("es"))));
        return op;
    }

    @Test
    void returnsPrimaryAndSortedSupportedForAnyMember() {
        when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator()));

        OperatorLocalesView view = useCase.execute(operatorId, callerId);

        verify(membershipCheck).ensureMember(callerId, operatorId);
        assertEquals("es", view.primaryLocale());
        assertEquals(List.of("en", "es"), view.supportedLocales());
    }

    @Test
    void nonMemberIs404() {
        doThrow(new ResourceNotFoundException("Tour operator not found"))
                .when(membershipCheck).ensureMember(callerId, operatorId);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(operatorId, callerId));
    }

    @Test
    void missingOperatorIs404() {
        when(operatorRepository.findById(operatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(operatorId, callerId));
    }
}
