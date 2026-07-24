package com.vointika.media.application.usecase;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeleteMediaUseCaseTest {

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
        mediaStoragePort = mock(MediaStoragePort.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new DeleteMediaUseCase(mediaRepository, mediaStoragePort, membershipCheck);
    }

    private Media media() {
        return new Media(mediaId, operatorId, "tour-operators/x/y.png",
                "image/png", 100, "y.png", UUID.randomUUID(), "Uma Uploader",
                Instant.parse("2026-07-21T00:00:00Z"));
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
    void objectDeleteFailureIsSwallowed() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.of(media()));
        doThrow(new RuntimeException("s3 down")).when(mediaStoragePort).deleteObject(anyString());

        // row is already gone; a failed object delete must not surface as a 500
        useCase.execute(operatorId, mediaId, callerId);
        verify(mediaRepository).deleteByIdAndTourOperatorId(mediaId, operatorId);
    }

    @Test
    void nonAdminIsRejectedBeforeAnyLookup() {
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
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
