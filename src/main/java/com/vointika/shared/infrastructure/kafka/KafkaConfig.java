package com.vointika.shared.infrastructure.kafka;

import com.vointika.shared.port.EventPublisherPort;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Wires the Kafka event backbone: the {@link EventPublisherPort} producer adapter,
 * the correlation-restoring consumer interceptor, and the dev topic definitions.
 * The Kafka client is confined to this package + the notification consumer
 * (ArchUnit-enforced), mirroring the other infrastructure fences.
 */
@Configuration
public class KafkaConfig {

    @Bean
    public EventPublisherPort kafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        return new KafkaEventPublisher(kafkaTemplate, EventTopics.BY_EVENT_TYPE);
    }

    @Bean
    public MdcCorrelationRecordInterceptor mdcCorrelationRecordInterceptor() {
        return new MdcCorrelationRecordInterceptor();
    }

    // Dev topic definitions — KafkaAdmin creates these at startup. One partition
    // suffices for local dev; prod sizing is an ops concern.
    @Bean
    NewTopic verificationEmailRequestedTopic() {
        return TopicBuilder.name(EventTopics.VERIFICATION_EMAIL_REQUESTED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic accountAlreadyRegisteredTopic() {
        return TopicBuilder.name(EventTopics.ACCOUNT_ALREADY_REGISTERED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic passwordChangedTopic() {
        return TopicBuilder.name(EventTopics.PASSWORD_CHANGED).partitions(1).replicas(1).build();
    }

    @Bean
    NewTopic passwordResetRequestedTopic() {
        return TopicBuilder.name(EventTopics.PASSWORD_RESET_REQUESTED).partitions(1).replicas(1).build();
    }
}
