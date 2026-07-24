package com.vointika.media.application.usecase;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.shared.port.UserAccountQuery;
import com.vointika.shared.port.UserContactView;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadMediaUseCaseTest {

    private MediaRepository mediaRepository;
    private MediaStoragePort mediaStoragePort;
    private TourOperatorMembershipCheck membershipCheck;
    private UserAccountQuery userAccountQuery;
    private UploadMediaUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID newId = UUID.fromString("019f8000-0000-7000-8000-000000000abc");

    private InputStream body() {
        return new ByteArrayInputStream("bytes".getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        mediaRepository = mock(MediaRepository.class);
        mediaStoragePort = mock(MediaStoragePort.class);
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        userAccountQuery = mock(UserAccountQuery.class);
        when(userAccountQuery.findContact(callerId))
                .thenReturn(Optional.of(new UserContactView("up@example.com", "Uma Uploader", "en")));
        IdGenerator idGenerator = mock(IdGenerator.class);
        when(idGenerator.newId()).thenReturn(newId);
        useCase = new UploadMediaUseCase(
                mediaRepository, mediaStoragePort, membershipCheck, userAccountQuery, idGenerator);
    }

    @Test
    void storesObjectBeforeRowUnderTenantNamespacedKey() {
        UUID id = useCase.execute(operatorId, callerId, "image/png", 1234, "My Photo.png", body());

        assertEquals(newId, id);
        verify(membershipCheck).ensureAdmin(callerId, operatorId);

        // object stored BEFORE the row is persisted
        InOrder order = inOrder(mediaStoragePort, mediaRepository);
        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        order.verify(mediaStoragePort).putObject(key.capture(), eq("image/png"), eq(1234L), any());
        order.verify(mediaRepository).save(any(Media.class));

        String storageKey = key.getValue();
        assertTrue(storageKey.startsWith("tour-operators/" + operatorId + "/" + newId + "-"));
        assertTrue(storageKey.endsWith("My_Photo.png"), "spaces sanitized: " + storageKey);

        ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(saved.capture());
        assertEquals(storageKey, saved.getValue().getStorageKey());
        assertEquals("image/png", saved.getValue().getContentType());
        assertEquals(callerId, saved.getValue().getCreatedBy());
        // The uploader's name is snapshotted onto the row.
        assertEquals("Uma Uploader", saved.getValue().getCreatedByName());
    }

    @Test
    void nonAdminIsRejectedBeforeAnyStorage() {
        doThrow(new ForbiddenException("This action requires ADMIN privileges"))
                .when(membershipCheck).ensureAdmin(callerId, operatorId);

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(operatorId, callerId, "image/png", 10, "x.png", body()));

        verify(mediaStoragePort, never()).putObject(anyString(), anyString(), anyLong(), any());
        verify(mediaRepository, never()).save(any());
    }

    @Test
    void rejectsDisallowedContentType() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, callerId, "text/plain", 10, "x.txt", body()));
        verify(mediaStoragePort, never()).putObject(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void rejectsEmptyAndOversizeFiles() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, callerId, "image/png", 0, "x.png", body()));
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(operatorId, callerId, "image/png",
                        26L * 1024 * 1024, "big.png", body()));
        verify(mediaStoragePort, never()).putObject(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void blankFilenameFallsBackToExtensionDerivedName() {
        useCase.execute(operatorId, callerId, "application/pdf", 10, "  ", body());
        ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(saved.capture());
        assertEquals("file.pdf", saved.getValue().getOriginalName());
    }
}
