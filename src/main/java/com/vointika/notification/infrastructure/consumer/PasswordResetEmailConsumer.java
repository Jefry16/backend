package com.vointika.notification.infrastructure.consumer;

import com.vointika.notification.application.usecase.SendNotificationUseCase;
import com.vointika.notification.infrastructure.config.NotificationProperties;
import com.vointika.shared.event.PasswordResetEmailRequestedEvent;
import com.vointika.shared.infrastructure.kafka.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes {@link PasswordResetEmailRequestedEvent} off Kafka and sends the
 * password-reset email. Fire-and-forget: failures are logged and swallowed.
 */
@Component
public class PasswordResetEmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetEmailConsumer.class);

    private final SendNotificationUseCase sendNotificationUseCase;
    private final NotificationProperties notificationProperties;

    public PasswordResetEmailConsumer(SendNotificationUseCase sendNotificationUseCase,
                                      NotificationProperties notificationProperties) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.notificationProperties = notificationProperties;
    }

    @KafkaListener(topics = EventTopics.PASSWORD_RESET_REQUESTED)
    public void handle(PasswordResetEmailRequestedEvent event) {
        try {
            Map<String, Object> variables = Map.of(
                    "name", event.name(),
                    "link", notificationProperties.passwordResetBaseUrl() + "?token=" + event.token());
            sendNotificationUseCase.execute("PASSWORD_RESET_EMAIL", event.email(), variables, event.locale());
        } catch (Exception e) {
            log.warn("Failed to send password reset email to {}: {}", event.email(), e.getMessage(), e);
        }
    }
}
