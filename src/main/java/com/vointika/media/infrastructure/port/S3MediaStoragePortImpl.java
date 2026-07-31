package com.vointika.media.infrastructure.port;

import com.vointika.media.application.port.MediaStoragePort;
import com.vointika.media.infrastructure.config.MediaS3Properties;
import com.vointika.shared.infrastructure.s3.S3Clients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Component
public class S3MediaStoragePortImpl implements MediaStoragePort {

    private static final Logger log = LoggerFactory.getLogger(S3MediaStoragePortImpl.class);

    private final S3Client s3Client;
    private final String bucket;

    // Two constructors now, so the injected one must say so.
    @Autowired
    public S3MediaStoragePortImpl(MediaS3Properties s3Properties) {
        this.s3Client = S3Clients.create(s3Properties.region(), s3Properties.endpoint(),
                s3Properties.accessKey(), s3Properties.secretKey());
        this.bucket = s3Properties.bucket();
    }

    /** Test seam: the production constructor builds its own client from config,
     *  which would leave the best-effort delete below untestable. */
    S3MediaStoragePortImpl(S3Client s3Client, String bucket) {
        this.s3Client = s3Client;
        this.bucket = bucket;
    }

    @Override
    public void putObject(String key, String contentType, long size, InputStream body) {
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .contentLength(size)
                        .build(),
                RequestBody.fromInputStream(body, size));
    }

    @Override
    public void deleteObject(String key) {
        // Best effort by contract: the row is already gone, and object storage
        // cannot join the transaction that removed it. A failure here leaves an
        // orphan to sweep, which is a smaller problem than failing the caller.
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (RuntimeException e) {
            log.warn("Failed to delete media object {}", key, e);
        }
    }
}
