package com.vointika.identity.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Identity's own binding of the shared {@code app.s3} settings (same bucket
 * as media — avatar keys live under {@code users/…}, media under
 * {@code tour-operators/…}). Duplicated per bounded context by design, like
 * media's {@code S3Properties} and theme's {@code ThemeS3Properties}.
 */
@ConfigurationProperties(prefix = "app.s3")
public record IdentityS3Properties(
        String region,
        String bucket,
        String endpoint,
        String accessKey,
        String secretKey
) {}
