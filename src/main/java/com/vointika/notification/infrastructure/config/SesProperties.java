package com.vointika.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param endpoint optional SESv2 endpoint override — set to a local mock in dev
 *                 (e.g. http://ses:8005); blank in prod so the SDK uses real SES.
 */
@ConfigurationProperties(prefix = "app.ses")
public record SesProperties(String region, String fromAddress, String endpoint) {}
