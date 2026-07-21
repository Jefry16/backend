package com.vointika.media.application.usecase;

import com.vointika.media.application.dto.output.MediaView;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetMediaUseCaseTest {

    private MediaRepository mediaRepository;
    private UserAccountQuery userAccountQuery;
    private TourOperatorMembershipCheck membershipCheck;
    private GetMediaUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID mediaId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID uploaderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mediaRepository = mock(MediaRepository.class);
        userAccountQuery = mock(UserAccountQuery.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new GetMediaUseCase(mediaRepository, userAccountQuery, membershipCheck);
    }

    private Media media() {
        return new Media(mediaId, operatorId, "tour-operators/x/y.png",
                "image/png", 100, "y.png", uploaderId, Instant.parse("2026-07-21T00:00:00Z"));
    }

    @Test
    void returnsTheMediaWithResolvedUploaderName() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.of(media()));
        when(userAccountQuery.findContact(uploaderId))
                .thenReturn(Optional.of(new UserContactView("up@example.com", "Uma Uploader", "en")));

        MediaView view = useCase.execute(operatorId, mediaId, callerId);

        verify(membershipCheck).ensureMember(callerId, operatorId);
        assertEquals(mediaId, view.id());
        assertEquals("tour-operators/x/y.png", view.storageKey());
        assertEquals(uploaderId, view.createdBy());
        assertEquals("Uma Uploader", view.createdByName());
    }

    @Test
    void uploaderNameNullWhenUnresolvable() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.of(media()));
        when(userAccountQuery.findContact(uploaderId)).thenReturn(Optional.empty());

        assertNull(useCase.execute(operatorId, mediaId, callerId).createdByName());
    }

    @Test
    void unknownOrCrossTenantIs404() {
        when(mediaRepository.findByIdAndTourOperatorId(mediaId, operatorId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(operatorId, mediaId, callerId));
    }
}
