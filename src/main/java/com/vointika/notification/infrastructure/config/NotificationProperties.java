package com.vointika.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Base URLs the identity email templates build their action links from. Only the
 * two identity flows exist in this slice; more base URLs are added here when the
 * events that need them (invitations, orders) land with their contexts.
 */
@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(String verificationBaseUrl,
                                     String passwordResetBaseUrl) {}
