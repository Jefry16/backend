package com.vointika.identity.infrastructure.port;

import com.vointika.identity.application.port.AvatarStoragePort;
import com.vointika.identity.infrastructure.config.IdentityS3Properties;
import com.vointika.shared.infrastructure.s3.S3Clients;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.InputStream;

@Component
public class S3AvatarStoragePortImpl implements AvatarStoragePort {

    private final S3Client s3Client;
    private final String bucket;

    public S3AvatarStoragePortImpl(IdentityS3Properties s3Properties) {
        this.s3Client = S3Clients.create(s3Properties.region(), s3Properties.endpoint(),
                s3Properties.accessKey(), s3Properties.secretKey());
        this.bucket = s3Properties.bucket();
    }

    @Override
    public void putObject(String key, String contentType, long size, InputStream body) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .contentLength(size)
                .build();
        s3Client.putObject(request, RequestBody.fromInputStream(body, size));
    }

    @Override
    public void deleteObject(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build());
    }
}
