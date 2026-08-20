package com.vointika.notification.infrastructure.consumer;

import com.vointika.notification.application.port.NotificationType;
import com.vointika.notification.application.usecase.SendNotificationUseCase;
import com.vointika.shared.event.TourOperatorWelcomeEmailRequestedEvent;
import com.vointika.shared.infrastructure.kafka.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes {@link TourOperatorWelcomeEmailRequestedEvent} off Kafka and sends the
 * operator-creator welcome email. Fire-and-forget: a send failure is logged and
 * swallowed so one bad address never stalls the partition.
 */
@Component
public class TourOperatorWelcomeEmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(TourOperatorWelcomeEmailConsumer.class);

    private final SendNotificationUseCase sendNotificationUseCase;

    public TourOperatorWelcomeEmailConsumer(SendNotificationUseCase sendNotificationUseCase) {
        this.sendNotificationUseCase = sendNotificationUseCase;
    }

    @KafkaListener(topics = EventTopics.TOUR_OPERATOR_WELCOME_EMAIL_REQUESTED)
    public void handle(TourOperatorWelcomeEmailRequestedEvent event) {
        try {
            Map<String, Object> variables = Map.of(
                    "name", event.name(),
                    "operatorName", event.operatorName());
            sendNotificationUseCase.execute(NotificationType.TOUR_OPERATOR_WELCOME_EMAIL, event.email(), variables, event.locale());
        } catch (Exception e) {
            log.warn("Failed to send welcome email to {}: {}", event.email(), e.getMessage(), e);
        }
    }
}
