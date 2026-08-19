package com.vointika.media.application.usecase;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.AuditTrailPort;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteMediaUseCaseTest {

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

    private MediaRepository mediaRepository;
    private MediaStoragePort mediaStoragePort;
    private TourOperatorMembershipCheck membershipCheck;
    private DeleteMediaUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID mediaId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mediaRepository = mock(MediaRepository.class);
        // requireByIdAndTourOperatorId is a default method: Mockito would stub it to
        // null and the 404 assertions below would pass without running the branch.
        doCallRealMethod().when(mediaRepository).requireByIdAndTourOperatorId(any(), any());
        mediaStoragePort = mock(MediaStoragePort.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new DeleteMediaUseCase(mediaRepository, mediaStoragePort, membershipCheck, transactionRunner, auditTrailPort);
    }

    private Media media() {
        return new Media(mediaId, operatorId, "tour-operators/x/y.png",
                "image/png", 100, "y.png", UUID.randomUUID(), "Uma Uploader",
                Instant.parse("2026-07-21T00:00:00Z"), null, null, null);
    }

    @Test
    void deletesRowThenObject() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.of(media()));

        useCase.execute(operatorId, mediaId, callerId);

        verify(membershipCheck).ensureAdmin(callerId, operatorId);
        verify(mediaRepository).deleteByIdAndTourOperatorId(mediaId, operatorId);
        verify(mediaStoragePort).deleteObject("tour-operators/x/y.png");
    }

    @Test
    void objectDeleteIsDelegatedAfterTheRowIsRemoved() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.of(media()));

        useCase.execute(operatorId, mediaId, callerId);

        // Resilience is the port's contract now — see S3MediaStoragePortImplTest.
        verify(mediaRepository).deleteByIdAndTourOperatorId(mediaId, operatorId);
        verify(mediaStoragePort).deleteObject(anyString());
    }

    @Test
    void nonAdminIsRejectedBeforeAnyLookup() {
        doThrow(new ForbiddenException(TourOperatorMembershipCheck.requiresRoleMessage("ADMIN")))
                .when(membershipCheck).ensureAdmin(callerId, operatorId);

        assertThrows(ForbiddenException.class, () -> useCase.execute(operatorId, mediaId, callerId));
        verify(mediaRepository, never()).findByIdAndTourOperatorId(any(), any());
        verify(mediaRepository, never()).deleteByIdAndTourOperatorId(any(), any());
    }

    @Test
    void unknownOrCrossTenantIs404() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(operatorId, mediaId, callerId));
        verify(mediaRepository, never()).deleteByIdAndTourOperatorId(any(), any());
    }
}
