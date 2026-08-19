package com.vointika.media.application.usecase;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.domain.entity.Media;
import com.vointika.media.domain.repository.MediaRepository;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.port.AuditTrailPort;
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
import com.vointika.media.application.port.ImageDimensionsPort;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UploadMediaUseCaseTest {

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
    private ImageDimensionsPort imageDimensionsPort;
    private TourOperatorMembershipCheck membershipCheck;
    private UserAccountQuery userAccountQuery;
    private UploadMediaUseCase useCase;

    private final UUID operatorId = UUID.randomUUID();
    private final UUID callerId = UUID.randomUUID();
    private final UUID newId = UUID.fromString("019f8000-0000-7000-8000-000000000abc");

    /**
     * A Supplier, because the use case reads the bytes twice — once to measure the
     * image, once to store it. Each call hands back a fresh stream, which is what
     * the container's spooled multipart does in production.
     */
    private Supplier<InputStream> body() {
        return () -> new ByteArrayInputStream("bytes".getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void setUp() {
        mediaRepository = mock(MediaRepository.class);
        mediaStoragePort = mock(MediaStoragePort.class);
        imageDimensionsPort = mock(ImageDimensionsPort.class);
        when(imageDimensionsPort.measure(any(), any())).thenReturn(Optional.empty());
        membershipCheck = mock(TourOperatorMembershipCheck.class);
        userAccountQuery = mock(UserAccountQuery.class);
        when(userAccountQuery.findContact(callerId))
                .thenReturn(Optional.of(new UserContactView("up@example.com", "Uma Uploader", "en")));
        IdGenerator idGenerator = mock(IdGenerator.class);
        when(idGenerator.newId()).thenReturn(newId);
        useCase = new UploadMediaUseCase(
                mediaRepository, mediaStoragePort, imageDimensionsPort, membershipCheck, userAccountQuery, idGenerator, transactionRunner, auditTrailPort);
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
        doThrow(new ForbiddenException(TourOperatorMembershipCheck.requiresRoleMessage("ADMIN")))
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

    @Test
    void measuredDimensionsAreStoredOnTheRow() {
        when(imageDimensionsPort.measure(any(), eq("image/png")))
                .thenReturn(Optional.of(new ImageDimensionsPort.Dimensions(400, 200)));

        useCase.execute(operatorId, callerId, "image/png", 1234, "photo.png", body());

        ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(saved.capture());
        assertThat(saved.getValue().getWidth()).isEqualTo(400);
        assertThat(saved.getValue().getHeight()).isEqualTo(200);
    }

    @Test
    void anUnmeasurableFileStillUploads() {
        // A PDF, or an image whose header is damaged. The columns stay null and
        // the upload succeeds — the port never fails a upload.
        when(imageDimensionsPort.measure(any(), any())).thenReturn(Optional.empty());

        useCase.execute(operatorId, callerId, "application/pdf", 1234, "terms.pdf", body());

        ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(saved.capture());
        assertThat(saved.getValue().getWidth()).isNull();
        assertThat(saved.getValue().getHeight()).isNull();
    }

    @Test
    void theBytesAreReadTwiceAndMeasuredBeforeTheyAreStored() {
        // Measuring after the object is stored would leave a stored file whose row
        // never got written if the measurement blew up. Order is the guard.
        useCase.execute(operatorId, callerId, "image/png", 1234, "photo.png", body());

        InOrder order = inOrder(imageDimensionsPort, mediaStoragePort);
        order.verify(imageDimensionsPort).measure(any(), eq("image/png"));
        order.verify(mediaStoragePort).putObject(anyString(), anyString(), anyLong(), any());
    }

    @Test
    void anUploadCarriesNoAltBecauseOnlyTheUploaderKnowsIt() {
        useCase.execute(operatorId, callerId, "image/png", 1234, "photo.png", body());

        ArgumentCaptor<Media> saved = ArgumentCaptor.forClass(Media.class);
        verify(mediaRepository).save(saved.capture());
        assertThat(saved.getValue().getAlt()).isNull();
    }

    @Test
    void bothStreamsAreClosed() {
        // The use case opens two streams (one to measure, one to store) and neither
        // consumer closes them: ImageInputStream.close() explicitly does NOT close
        // its source, and the S3 SDK does not close a caller's stream. On a
        // disk-spooled multipart each leak is a file descriptor, one per upload.
        List<TrackedStream> opened = new ArrayList<>();
        Supplier<InputStream> tracking = () -> {
            TrackedStream s = new TrackedStream("bytes".getBytes(StandardCharsets.UTF_8));
            opened.add(s);
            return s;
        };

        useCase.execute(operatorId, callerId, "image/png", 1234, "photo.png", tracking);

        assertThat(opened).hasSize(2);
        assertThat(opened).allSatisfy(s -> assertThat(s.closed)
                .withFailMessage("a stream the use case opened was never closed")
                .isTrue());
    }

    private static final class TrackedStream extends ByteArrayInputStream {
        private boolean closed;

        private TrackedStream(byte[] bytes) {
            super(bytes);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }
}
