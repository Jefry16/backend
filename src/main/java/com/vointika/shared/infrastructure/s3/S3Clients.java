package com.vointika.shared.infrastructure.s3;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

/**
 * Builds an {@link S3Client} from raw connection settings. Shared by every
 * context that talks to S3-compatible object storage (today {@code media} and
 * {@code theme}) so the client-construction logic — endpoint override,
 * path-style addressing, dev-vs-prod credential selection — lives in one place.
 *
 * <p>Takes primitive strings rather than any context's {@code *S3Properties}
 * record, so it stays a neutral cross-cutting helper and never imports a
 * bounded context's types.
 *
 * <ul>
 *   <li><b>Dev (MinIO / S3-compatible):</b> a non-blank {@code endpoint} forces
 *       an endpoint override + path-style addressing, with static credentials
 *       when an access key is supplied.</li>
 *   <li><b>Prod (AWS):</b> a blank/absent {@code endpoint} leaves the SDK's
 *       default credential provider chain (env vars / IAM role) and the standard
 *       AWS endpoints in place.</li>
 * </ul>
 */
public final class S3Clients {

    private S3Clients() {
    }

    public static S3Client create(String region, String endpoint, String accessKey, String secretKey) {
        S3ClientBuilder builder = S3Client.builder().region(Region.of(region));

        if (endpoint != null && !endpoint.isBlank()) {
            builder = builder
                    .endpointOverride(URI.create(endpoint))
                    .forcePathStyle(true);

            if (accessKey != null && !accessKey.isBlank()) {
                builder = builder.credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));
            }
        }

        return builder.build();
    }
}
