package com.vointika.touroperator.application.usecase;

import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.shared.valueobject.Slug;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClearOperatorLogoUseCaseTest {

    private TourOperatorRepository operatorRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private ClearOperatorLogoUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new ClearOperatorLogoUseCase(operatorRepository, membershipCheck);
        when(operatorRepository.save(any())).thenAnswer(a -> a.getArgument(0));
    }

    private TourOperator operatorWithLogo() {
        TourOperator op = new TourOperator(operatorId, new TourOperatorName("Acme"), new Slug("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("1 St"), UUID.randomUUID());
        op.setLogo(UUID.randomUUID());
        return op;
    }

    @Test
    void clearsTheLogo() {
        when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operatorWithLogo()));

        useCase.execute(operatorId, callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        ArgumentCaptor<TourOperator> saved = ArgumentCaptor.forClass(TourOperator.class);
        verify(operatorRepository).save(saved.capture());
        assertNull(saved.getValue().getLogoMediaId());
    }

    @Test
    void nonAdminIsRejectedBeforeAnyLookup() {
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
                .when(membershipCheck).ensureAdmin(callerId, operatorId);

        assertThrows(ForbiddenException.class, () -> useCase.execute(operatorId, callerId));
        verify(operatorRepository, never()).findById(any());
        verify(operatorRepository, never()).save(any());
    }

    @Test
    void missingOperatorIs404() {
        when(operatorRepository.findById(operatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(operatorId, callerId));
    }
}
