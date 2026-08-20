package com.vointika.notification.infrastructure.consumer;

import com.vointika.notification.application.port.NotificationType;
import com.vointika.notification.application.usecase.SendNotificationUseCase;
import com.vointika.shared.event.AccountAlreadyRegisteredEvent;
import com.vointika.shared.infrastructure.kafka.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes {@link AccountAlreadyRegisteredEvent} off Kafka and sends the
 * "an account already exists" notice (the anti-enumeration branch of register).
 * Fire-and-forget: failures are logged and swallowed.
 */
@Component
public class AccountAlreadyRegisteredConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountAlreadyRegisteredConsumer.class);

    private final SendNotificationUseCase sendNotificationUseCase;

    public AccountAlreadyRegisteredConsumer(SendNotificationUseCase sendNotificationUseCase) {
        this.sendNotificationUseCase = sendNotificationUseCase;
    }

    @KafkaListener(topics = EventTopics.ACCOUNT_ALREADY_REGISTERED)
    public void handle(AccountAlreadyRegisteredEvent event) {
        try {
            Map<String, Object> variables = Map.of("name", event.name());
            sendNotificationUseCase.execute(NotificationType.ACCOUNT_ALREADY_REGISTERED_EMAIL, event.email(), variables, event.locale());
        } catch (Exception e) {
            log.warn("Failed to send account-already-registered email to {}: {}",
                    event.email(), e.getMessage(), e);
        }
    }
}
