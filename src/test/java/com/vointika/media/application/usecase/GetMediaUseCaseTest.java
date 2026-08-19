package com.vointika.media.application.usecase;

import com.vointika.media.application.dto.output.MediaView;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetMediaUseCaseTest {

    private MediaRepository mediaRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private GetMediaUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID mediaId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID uploaderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mediaRepository = mock(MediaRepository.class);
        // requireByIdAndTourOperatorId is a default method: Mockito would stub it to
        // null and the 404 assertions below would pass without running the branch.
        doCallRealMethod().when(mediaRepository).requireByIdAndTourOperatorId(any(), any());
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new GetMediaUseCase(mediaRepository, membershipCheck);
    }

    private Media media() {
        return new Media(mediaId, operatorId, "tour-operators/x/y.png",
                "image/png", 100, "y.png", uploaderId, "Uma Uploader",
                Instant.parse("2026-07-21T00:00:00Z"), null, null, null);
    }

    @Test
    void returnsTheMediaWithSnapshotUploaderName() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.of(media()));

        MediaView view = useCase.execute(operatorId, mediaId, callerId);

        verify(membershipCheck).ensureMember(callerId, operatorId);
        assertEquals(mediaId, view.id());
        assertEquals("tour-operators/x/y.png", view.storageKey());
        assertEquals(uploaderId, view.createdBy());
        assertEquals("Uma Uploader", view.createdByName());
    }

    @Test
    void unknownOrCrossTenantIs404() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(operatorId, mediaId, callerId));
    }
}
