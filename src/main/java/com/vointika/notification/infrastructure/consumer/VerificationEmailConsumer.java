package com.vointika.notification.infrastructure.consumer;

import com.vointika.notification.application.port.NotificationType;
import com.vointika.notification.application.usecase.SendNotificationUseCase;
import com.vointika.notification.infrastructure.config.NotificationProperties;
import com.vointika.shared.event.VerificationEmailRequestedEvent;
import com.vointika.shared.infrastructure.kafka.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes {@link VerificationEmailRequestedEvent} off Kafka and sends the
 * account-verification email. Fire-and-forget: a send failure is logged and
 * swallowed so a single bad address never stalls the partition.
 */
@Component
public class VerificationEmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(VerificationEmailConsumer.class);

    private final SendNotificationUseCase sendNotificationUseCase;
    private final NotificationProperties notificationProperties;

    public VerificationEmailConsumer(SendNotificationUseCase sendNotificationUseCase,
                                     NotificationProperties notificationProperties) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.notificationProperties = notificationProperties;
    }

    @KafkaListener(topics = EventTopics.VERIFICATION_EMAIL_REQUESTED)
    public void handle(VerificationEmailRequestedEvent event) {
        try {
            Map<String, Object> variables = Map.of(
                    "name", event.name(),
                    "link", notificationProperties.verificationBaseUrl() + "?token=" + event.token());
            sendNotificationUseCase.execute(NotificationType.VERIFICATION_EMAIL, event.email(), variables, event.locale());
        } catch (Exception e) {
            log.warn("Failed to send verification email to {}: {}", event.email(), e.getMessage(), e);
        }
    }
}
