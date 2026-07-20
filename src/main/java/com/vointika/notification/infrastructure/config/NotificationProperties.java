package com.vointika.notification.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Base URLs the email templates build their action links from. More base URLs
 * are added here when the events that need them land with their contexts.
 */
@ConfigurationProperties(prefix = "app.notification")
public record NotificationProperties(String verificationBaseUrl,
                                     String passwordResetBaseUrl,
                                     String invitationAcceptBaseUrl) {}
