package com.vointika.touroperator.application.usecase;

import com.vointika.reference.domain.repository.LanguageRepository;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.touroperator.domain.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UpdateOperatorLocalesUseCaseTest {

    private TourOperatorRepository operatorRepository;
    private LanguageRepository languageRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private UpdateOperatorLocalesUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        languageRepository = mock(LanguageRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new UpdateOperatorLocalesUseCase(operatorRepository, languageRepository, membershipCheck);
        when(operatorRepository.save(any())).thenAnswer(a -> a.getArgument(0));
        // by default the master list knows en/es/fr
        when(languageRepository.existsByCode("en")).thenReturn(true);
        when(languageRepository.existsByCode("es")).thenReturn(true);
        when(languageRepository.existsByCode("fr")).thenReturn(true);
    }

    private TourOperator operator() {
        return new TourOperator(operatorId, new TourOperatorName("Acme"), new Slug("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("1 St"), UUID.randomUUID());
    }

    @Test
    void setsPrimaryAndSupportedWhenAllValid() {
        when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator()));

        useCase.execute(operatorId, "es", List.of("en", "es", "fr"), callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        ArgumentCaptor<TourOperator> saved = ArgumentCaptor.forClass(TourOperator.class);
        verify(operatorRepository).save(saved.capture());
        assertEquals("es", saved.getValue().getPrimaryLocale().value());
        assertEquals(3, saved.getValue().getSupportedLocales().size());
    }

    @Test
    void unknownLanguageCodeIs422() {
        when(languageRepository.existsByCode("xx")).thenReturn(false);

        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, "en", List.of("en", "xx"), callerId));
        verify(operatorRepository, never()).save(any());
    }

    @Test
    void badShapeLocaleIs422() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, "en_US", List.of("en"), callerId));
        verify(operatorRepository, never()).save(any());
    }

    @Test
    void primaryNotInSupportedIs422() {
        when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator()));

        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, "fr", List.of("en", "es"), callerId));
        verify(operatorRepository, never()).save(any());
    }

    @Test
    void emptySupportedIs422() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, "en", List.of(), callerId));
        verify(operatorRepository, never()).save(any());
    }

    @Test
    void nonAdminIsRejectedBeforeAnyValidation() {
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
                .when(membershipCheck).ensureAdmin(callerId, operatorId);

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(operatorId, "en", List.of("en"), callerId));
        verify(languageRepository, never()).existsByCode(any());
        verify(operatorRepository, never()).save(any());
    }

    @Test
    void missingOperatorIs404() {
        when(operatorRepository.findById(operatorId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.execute(operatorId, "en", List.of("en"), callerId));
    }
}
