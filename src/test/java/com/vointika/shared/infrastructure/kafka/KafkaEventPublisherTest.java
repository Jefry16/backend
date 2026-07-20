package com.vointika.shared.infrastructure.kafka;

import com.vointika.shared.event.PasswordChangedEvent;
import com.vointika.shared.event.VerificationEmailRequestedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Header;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@SuppressWarnings({"unchecked", "rawtypes"})
class KafkaEventPublisherTest {

    private final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
    private final KafkaEventPublisher publisher =
            new KafkaEventPublisher(template, EventTopics.BY_EVENT_TYPE);

    @Test
    void routesEventToItsTopicKeyedByEmail() {
        var event = new VerificationEmailRequestedEvent("user@example.com", "Ada", "tok123", "en");

        publisher.publish(event);

        ArgumentCaptor<ProducerRecord> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(captor.capture());
        ProducerRecord record = captor.getValue();
        assertEquals(EventTopics.VERIFICATION_EMAIL_REQUESTED, record.topic());
        assertEquals("user@example.com", record.key());
        assertSame(event, record.value());
    }

    @Test
    void carriesTheCorrelationIdFromMdcAsAHeader() {
        MDC.put("requestId", "corr-42");
        try {
            publisher.publish(new PasswordChangedEvent("a@b.com", "Ada", "en"));
        } finally {
            MDC.remove("requestId");
        }

        ArgumentCaptor<ProducerRecord> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(template).send(captor.capture());
        Header header = captor.getValue().headers().lastHeader("X-Correlation-Id");
        assertNotNull(header);
        assertEquals("corr-42", new String(header.value(), StandardCharsets.UTF_8));
    }

    @Test
    void failsFastOnAnUnmappedEvent() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> publisher.publish("not-a-mapped-event"));
        assertEquals(true, ex.getMessage().contains("No Kafka topic mapped"));
    }
}
