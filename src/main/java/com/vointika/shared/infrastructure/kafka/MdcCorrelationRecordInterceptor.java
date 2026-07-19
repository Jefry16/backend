package com.vointika.shared.infrastructure.kafka;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.MDC;
import org.springframework.kafka.listener.RecordInterceptor;

import java.nio.charset.StandardCharsets;

/**
 * Restores the producer's correlation id (a Kafka header) into the consumer
 * thread's MDC so email-send logs correlate back to the request that triggered
 * the event — the cross-broker equivalent of the in-process MdcTaskDecorator.
 * Spring Boot auto-applies a single {@code RecordInterceptor} bean to the
 * listener container factory.
 */
public class MdcCorrelationRecordInterceptor implements RecordInterceptor<Object, Object> {

    private static final String MDC_KEY = "requestId";

    @Override
    public ConsumerRecord<Object, Object> intercept(ConsumerRecord<Object, Object> record,
                                                     Consumer<Object, Object> consumer) {
        Header header = record.headers().lastHeader(KafkaEventPublisher.CORRELATION_HEADER);
        if (header != null && header.value() != null) {
            MDC.put(MDC_KEY, new String(header.value(), StandardCharsets.UTF_8));
        }
        return record;
    }

    @Override
    public void afterRecord(ConsumerRecord<Object, Object> record, Consumer<Object, Object> consumer) {
        MDC.remove(MDC_KEY);
    }
}
