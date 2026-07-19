package com.vointika.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ses")
public record SesProperties(String region, String fromAddress) {}
