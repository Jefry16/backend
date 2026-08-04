package com.vointika.touroperator.application.usecase;

import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.MediaAssetBatchQuery;
import com.vointika.shared.port.MediaAssetBatchQuery.MediaAsset;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.entity.TourOperator;
import com.vointika.touroperator.domain.repository.TourOperatorBrandRepository;
import com.vointika.touroperator.domain.repository.TourOperatorRepository;
import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.domain.valueobject.TourOperatorAddress;
import com.vointika.touroperator.domain.valueobject.TourOperatorName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetOperatorLogoUseCaseTest {

    // Executes the work inline so assertions on the wrapped calls still hold.
    private final TransactionRunner transactionRunner = executingRunner();

    private static TransactionRunner executingRunner() {
        TransactionRunner runner = mock(TransactionRunner.class);
        when(runner.call(any())).thenAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(0)).get());
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(runner).run(any());
        return runner;
    }

    private final AuditTrailPort auditTrailPort = mock(AuditTrailPort.class);

    private TourOperatorRepository operatorRepository;
    private TourOperatorBrandRepository brandRepository;
    private MediaAssetBatchQuery mediaAssetBatchQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private SetOperatorLogoUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID mediaId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        operatorRepository = mock(TourOperatorRepository.class);
        brandRepository = mock(TourOperatorBrandRepository.class);
        mediaAssetBatchQuery = mock(MediaAssetBatchQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new SetOperatorLogoUseCase(operatorRepository, brandRepository, mediaAssetBatchQuery,
                membershipCheck, transactionRunner, auditTrailPort);
        when(brandRepository.findLogoMediaId(operatorId)).thenReturn(Optional.empty());
    }

    private TourOperator operator() {
        return new TourOperator(operatorId, new TourOperatorName("Acme"), new Handle("acme"),
                UUID.randomUUID(), UUID.randomUUID(), new TourOperatorAddress("1 St"), UUID.randomUUID());
    }

    @Test
    void setsLogoWhenMediaBelongsToOperator() {
        when(mediaAssetBatchQuery.findAssetsByIds(operatorId, java.util.Set.of(mediaId)))
                .thenReturn(Map.of(mediaId, new MediaAsset("tour-operators/x/logo.png", null, null, null)));
        when(operatorRepository.findById(operatorId)).thenReturn(Optional.of(operator()));

        useCase.execute(operatorId, mediaId, callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        // The logo is the brand's column since V10, not the operator's.
        ArgumentCaptor<UUID> saved = ArgumentCaptor.forClass(UUID.class);
        verify(brandRepository).setLogoMediaId(eq(operatorId), saved.capture());
        assertEquals(mediaId, saved.getValue());
    }

    @Test
    void foreignOrUnknownMediaIs422() {
        when(mediaAssetBatchQuery.findAssetsByIds(operatorId, java.util.Set.of(mediaId)))
                .thenReturn(Map.of()); // not owned by this operator

        assertThrows(InvalidFieldException.class, () -> useCase.execute(operatorId, mediaId, callerId));
        verify(brandRepository, never()).setLogoMediaId(any(), any());
    }

    @Test
    void nullMediaIdIs422() {
        assertThrows(InvalidFieldException.class, () -> useCase.execute(operatorId, null, callerId));
        verify(brandRepository, never()).setLogoMediaId(any(), any());
    }

    @Test
    void nonAdminIsRejectedBeforeAnyValidation() {
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
                .when(membershipCheck).ensureAdmin(callerId, operatorId);

        assertThrows(ForbiddenException.class, () -> useCase.execute(operatorId, mediaId, callerId));
        verify(mediaAssetBatchQuery, never()).findAssetsByIds(any(), any());
        verify(brandRepository, never()).setLogoMediaId(any(), any());
    }

    @Test
    void missingOperatorIs404() {
        when(mediaAssetBatchQuery.findAssetsByIds(operatorId, java.util.Set.of(mediaId)))
                .thenReturn(Map.of(mediaId, new MediaAsset("tour-operators/x/logo.png", null, null, null)));
        when(operatorRepository.findById(operatorId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(operatorId, mediaId, callerId));
    }
}
