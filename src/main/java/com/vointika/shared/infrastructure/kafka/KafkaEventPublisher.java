package com.vointika.shared.infrastructure.kafka;

import com.vointika.shared.port.EventPublisherPort;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Publishes domain events onto Kafka behind the transport-agnostic
 * {@link EventPublisherPort} seam — use cases call {@code publish(event)} and
 * never see Kafka. The event's runtime class selects the topic (fail-fast if
 * unmapped: publishing an unmapped event is a wiring bug, not a runtime
 * condition). The partition key is the event's {@code email()} when present, so a
 * given recipient's events stay ordered; the request correlation id rides a
 * header so consumer-side logs correlate to the originating request.
 */
public class KafkaEventPublisher implements EventPublisherPort {

    static final String CORRELATION_HEADER = "X-Correlation-Id";
    private static final String MDC_KEY = "requestId";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Map<Class<?>, String> topicsByEventType;

    public KafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                               Map<Class<?>, String> topicsByEventType) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicsByEventType = topicsByEventType;
    }

    @Override
    public void publish(Object event) {
        String topic = topicsByEventType.get(event.getClass());
        if (topic == null) {
            throw new IllegalStateException(
                    "No Kafka topic mapped for event " + event.getClass().getName());
        }
        ProducerRecord<String, Object> record =
                new ProducerRecord<>(topic, partitionKey(event), event);
        String correlationId = MDC.get(MDC_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            record.headers().add(new RecordHeader(
                    CORRELATION_HEADER, correlationId.getBytes(StandardCharsets.UTF_8)));
        }
        kafkaTemplate.send(record);
    }

    /** Best-effort per-recipient ordering key: the event's {@code email()} if it has one. */
    private static String partitionKey(Object event) {
        try {
            Method email = event.getClass().getMethod("email");
            Object value = email.invoke(event);
            return value == null ? null : value.toString();
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}
