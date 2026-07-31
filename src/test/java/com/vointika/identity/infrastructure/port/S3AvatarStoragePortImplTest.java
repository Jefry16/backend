package com.vointika.identity.infrastructure.port;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class S3AvatarStoragePortImplTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final S3AvatarStoragePortImpl port = new S3AvatarStoragePortImpl(s3Client, "bucket");

    /**
     * The row this object belonged to is already gone and storage cannot join that
     * transaction, so a failure here must not reach the caller — it leaves an orphan
     * to sweep, which is the smaller problem. The use cases used to catch this; the
     * contract now lives on the port and is kept here.
     */
    @Test
    void deleteIsBestEffortAndNeverThrows() {
        doThrow(new RuntimeException("s3 down")).when(s3Client).deleteObject(any(DeleteObjectRequest.class));

        assertDoesNotThrow(() -> port.deleteObject("some/avatar/key"));
    }

    @Test
    void deleteDelegatesToS3() {
        port.deleteObject("some/avatar/key");

        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }
}
