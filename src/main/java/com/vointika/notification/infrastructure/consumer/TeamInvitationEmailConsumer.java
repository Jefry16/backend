package com.vointika.notification.infrastructure.consumer;

import com.vointika.notification.application.usecase.SendNotificationUseCase;
import com.vointika.notification.infrastructure.config.NotificationProperties;
import com.vointika.shared.event.TeamInvitationRequestedEvent;
import com.vointika.shared.infrastructure.kafka.EventTopics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Consumes {@link TeamInvitationRequestedEvent} off Kafka and sends the team
 * invitation email (with the accept link built from the raw token). Fire-and-
 * forget: a send failure is logged and swallowed so one bad address never stalls
 * the partition.
 */
@Component
public class TeamInvitationEmailConsumer {

    private static final Logger log = LoggerFactory.getLogger(TeamInvitationEmailConsumer.class);

    private final SendNotificationUseCase sendNotificationUseCase;
    private final NotificationProperties notificationProperties;

    public TeamInvitationEmailConsumer(SendNotificationUseCase sendNotificationUseCase,
                                       NotificationProperties notificationProperties) {
        this.sendNotificationUseCase = sendNotificationUseCase;
        this.notificationProperties = notificationProperties;
    }

    @KafkaListener(topics = EventTopics.TEAM_INVITATION_REQUESTED)
    public void handle(TeamInvitationRequestedEvent event) {
        try {
            Map<String, Object> variables = Map.of(
                    "operatorName", event.operatorName(),
                    "role", event.role(),
                    "link", notificationProperties.invitationAcceptBaseUrl() + "?token=" + event.token());
            sendNotificationUseCase.execute(
                    "TEAM_INVITATION_EMAIL", event.email(), variables, event.locale());
        } catch (Exception e) {
            log.warn("Failed to send team invitation email to {}: {}", event.email(), e.getMessage(), e);
        }
    }
}
