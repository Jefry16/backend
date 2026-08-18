package com.vointika.media.application.usecase;

import com.vointika.media.application.dto.output.MediaView;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.FilterSpec;
import com.vointika.shared.list.ListQuery;
import com.vointika.shared.list.SortDirection;
import com.vointika.shared.list.SortSpec;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ListMediaUseCaseTest {

    private MediaRepository mediaRepository;
    private TourOperatorMembershipCheck membershipCheck;
    private ListMediaUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID uploaderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        mediaRepository = mock(MediaRepository.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        useCase = new ListMediaUseCase(mediaRepository, membershipCheck);
    }

    private ListQuery query() {
        return new ListQuery(operatorId, FilterSpec.empty(),
                new SortSpec("createdAt", SortDirection.DESC), null);
    }

    private Media media() {
        return new Media(UUID.randomUUID(), operatorId, "tour-operators/x/y.png",
                "image/png", 100, "y.png", uploaderId, "Uma Uploader",
                Instant.parse("2026-07-21T00:00:00Z"), null, null, null);
    }

    @Test
    void mapsRowsWithSnapshotUploaderNameAndPropagatesCursor() {
        when(mediaRepository.list(any()))
                .thenReturn(new CursorPage<>(List.of(media(), media()), "next"));

        CursorPage<MediaView> page = useCase.execute(query(), callerId);

        verify(membershipCheck).ensureMember(callerId, operatorId);
        assertEquals(2, page.data().size());
        assertEquals("next", page.nextCursor());
        assertEquals(uploaderId, page.data().get(0).createdBy());
        assertEquals("Uma Uploader", page.data().get(0).createdByName());
    }

    @Test
    void emptyPageStaysEmpty() {
        when(mediaRepository.list(any())).thenReturn(new CursorPage<>(List.of(), null));
        assertTrue(useCase.execute(query(), callerId).data().isEmpty());
    }

    @Test
    void nonMemberIs404BeforeAnyQuery() {
        doThrow(new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND))
                .when(membershipCheck).ensureMember(callerId, operatorId);

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(query(), callerId));
        verify(mediaRepository, never()).list(any());
    }
}
