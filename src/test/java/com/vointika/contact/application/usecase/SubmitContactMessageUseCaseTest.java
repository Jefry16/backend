package com.vointika.contact.application.usecase;

import com.vointika.contact.domain.entity.ContactMessage;
import com.vointika.contact.domain.repository.ContactMessageRepository;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.exception.TooManyRequestsException;
import com.vointika.shared.port.RateLimiterPort;
import com.vointika.shared.port.StorefrontOperatorQuery;
import com.vointika.shared.port.StorefrontOperatorView;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubmitContactMessageUseCaseTest {

    private static final String SLUG = "acme";
    private static final UUID OP = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3ea2");
    private static final UUID NEW_ID = UUID.fromString("019f7f33-1833-7dc1-b008-47e6c68b3f01");

    private ContactMessageRepository messageRepository;
    private RateLimiterPort rateLimiter;
    private SubmitContactMessageUseCase useCase;

    @BeforeEach
    void setUp() {
        messageRepository = mock(ContactMessageRepository.class);
        rateLimiter = mock(RateLimiterPort.class);
        StorefrontOperatorQuery operatorQuery = mock(StorefrontOperatorQuery.class);
        IdGenerator idGenerator = mock(IdGenerator.class);

        when(operatorQuery.findBySlug(SLUG)).thenReturn(Optional.of(new StorefrontOperatorView(
                OP, "Acme Tours", SLUG, null, "en", List.of("en"), "EUR",
                "Europe/Madrid", false, null)));
        when(rateLimiter.tryAcquire(any(), anyInt(), any())).thenReturn(true);
        when(idGenerator.newId()).thenReturn(NEW_ID);

        useCase = new SubmitContactMessageUseCase(
                messageRepository, operatorQuery, rateLimiter, idGenerator);
    }

    private void submit(String name, String email, String summary, String content) {
        useCase.execute(SLUG, name, email, summary, content);
    }

    private ContactMessage saved() {
        ArgumentCaptor<ContactMessage> captor = ArgumentCaptor.forClass(ContactMessage.class);
        verify(messageRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    void stores_the_message_against_the_tenant_addressed_by_slug() {
        submit("Laura", "laura@example.com", "Child seats?", "Do you have child seats?");

        ContactMessage message = saved();
        assertThat(message.getTourOperatorId()).isEqualTo(OP);
        assertThat(message.getName()).isEqualTo("Laura");
        assertThat(message.getEmail()).isEqualTo("laura@example.com");
        assertThat(message.getSummary()).isEqualTo("Child seats?");
        assertThat(message.getContent()).isEqualTo("Do you have child seats?");
    }

    @Test
    void arrives_unread() {
        // The entire point of an inbox is that someone still has to look.
        submit("Laura", "laura@example.com", "Hi", "Hello");

        assertThat(saved().getReadAt()).isNull();
    }

    @Test
    void a_shopper_may_leave_their_name_out() {
        submit(null, "laura@example.com", "Hi", "Hello");

        assertThat(saved().getName()).isNull();
    }

    @Test
    void a_blank_name_is_no_name_rather_than_an_empty_string() {
        submit("   ", "laura@example.com", "Hi", "Hello");

        assertThat(saved().getName()).isNull();
    }

    @Test
    void trims_what_the_shopper_typed() {
        submit("  Laura  ", "  laura@example.com ", " Child seats? ", "  Do you? ");

        ContactMessage message = saved();
        assertThat(message.getName()).isEqualTo("Laura");
        assertThat(message.getEmail()).isEqualTo("laura@example.com");
        assertThat(message.getSummary()).isEqualTo("Child seats?");
    }

    @Test
    void refuses_a_message_with_no_reply_address() {
        // An inbox message the operator cannot answer is worthless.
        assertThatThrownBy(() -> submit("Laura", "not-an-email", "Hi", "Hello"))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> submit("Laura", null, "Hi", "Hello"))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void refuses_an_empty_subject_or_body() {
        assertThatThrownBy(() -> submit("Laura", "laura@example.com", "  ", "Hello"))
                .isInstanceOf(InvalidFieldException.class);
        assertThatThrownBy(() -> submit("Laura", "laura@example.com", "Hi", "  "))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void caps_the_body_so_a_public_form_cannot_fill_the_column() {
        assertThatThrownBy(() ->
                submit("Laura", "laura@example.com", "Hi", "x".repeat(5001)))
                .isInstanceOf(InvalidFieldException.class);
    }

    @Test
    void throttles_per_tenant_and_stores_nothing_when_it_trips() {
        when(rateLimiter.tryAcquire(eq("rl:contact:tenant:" + OP), anyInt(), any(Duration.class)))
                .thenReturn(false);

        assertThatThrownBy(() -> submit("Laura", "laura@example.com", "Hi", "Hello"))
                .isInstanceOf(TooManyRequestsException.class);
        verify(messageRepository, never()).save(any());
    }

    @Test
    void an_unknown_storefront_is_not_found() {
        assertThatThrownBy(() -> useCase.execute("nobody", null, "a@b.com", "Hi", "Hello"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(messageRepository, never()).save(any());
    }
}
