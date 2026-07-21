package com.vointika.media.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Media's own binding of the shared {@code app.s3} settings (same bucket as
 * avatars — media keys live under {@code tour-operators/…}, avatars under
 * {@code users/…}). Duplicated per bounded context by design, like identity's
 * {@code IdentityS3Properties}.
 */
@ConfigurationProperties(prefix = "app.s3")
public record MediaS3Properties(
        String region,
        String bucket,
        String endpoint,
        String accessKey,
        String secretKey
) {}
