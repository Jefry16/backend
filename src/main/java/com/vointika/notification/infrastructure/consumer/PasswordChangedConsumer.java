package com.vointika.notification.infrastructure.consumer;

import com.vointika.notification.application.usecase.SendNotificationUseCase;
import com.vointika.shared.event.PasswordChangedEvent;
import com.vointika.shared.infrastructure.kafka.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes {@link PasswordChangedEvent} off Kafka and sends the
 * password-changed security notice. Fire-and-forget: failures are logged and
 * swallowed.
 */
@Component
public class PasswordChangedConsumer {

    private static final Logger log = LoggerFactory.getLogger(PasswordChangedConsumer.class);

    private final SendNotificationUseCase sendNotificationUseCase;

    public PasswordChangedConsumer(SendNotificationUseCase sendNotificationUseCase) {
        this.sendNotificationUseCase = sendNotificationUseCase;
    }

    @KafkaListener(topics = EventTopics.PASSWORD_CHANGED)
    public void handle(PasswordChangedEvent event) {
        try {
            Map<String, Object> variables = Map.of("name", event.name());
            sendNotificationUseCase.execute("PASSWORD_CHANGED_EMAIL", event.email(), variables, event.locale());
        } catch (Exception e) {
            log.warn("Failed to send password changed email to {}: {}", event.email(), e.getMessage(), e);
        }
    }
}
