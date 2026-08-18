package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.RequestPasswordResetInput;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.shared.event.PasswordResetEmailRequestedEvent;
import com.vointika.identity.domain.entity.PasswordResetToken;
import com.vointika.identity.domain.entity.User;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.identity.domain.repository.PasswordResetTokenRepository;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.shared.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.shared.port.RateLimiterPort;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestPasswordResetUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private TokenGeneratorPort tokenGenerator;
    @Mock private TokenHasherPort tokenHasher;
    @Mock private EventPublisherPort eventPublisher;
    @Mock private RateLimiterPort rateLimiter;

    private final IdGenerator idGenerator = UUID::randomUUID;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public <T> T call(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void run(Runnable work) { work.run(); }
    };

    private RequestPasswordResetUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RequestPasswordResetUseCase(userRepository, passwordResetTokenRepository,
                tokenGenerator, tokenHasher, eventPublisher, transactionRunner, idGenerator, rateLimiter);
    }

    @Test
    void requestPersistsHashedTokenAndPublishesRawTokenInEvent() {
        User user = new User(UUID.randomUUID(), new Email("test@example.com"), new UserName("Test"), "hashed");
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(tokenGenerator.generatePasswordResetToken()).thenReturn("raw-reset");
        when(tokenHasher.hash("raw-reset")).thenReturn("hashed-reset");

        useCase.execute(new RequestPasswordResetInput("test@example.com"));

        verify(passwordResetTokenRepository).expireAllByUserId(user.getId());

        ArgumentCaptor<PasswordResetToken> savedToken = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(savedToken.capture());
        assertEquals("hashed-reset", savedToken.getValue().getTokenHash());

        ArgumentCaptor<PasswordResetEmailRequestedEvent> publishedEvent =
                ArgumentCaptor.forClass(PasswordResetEmailRequestedEvent.class);
        verify(eventPublisher).publish(publishedEvent.capture());
        assertEquals("raw-reset", publishedEvent.getValue().token());
    }

    @Test
    void shouldDoNothingWhenUserNotFound() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> useCase.execute(new RequestPasswordResetInput("test@example.com")));

        // Timing equalization (§7.5): token work runs, but nothing persists
        // and no email event is published.
        verify(tokenGenerator).generatePasswordResetToken();
        verify(passwordResetTokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldSilentlyNoOpWhenRateLimited() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(false);

        assertDoesNotThrow(() -> useCase.execute(new RequestPasswordResetInput("test@example.com")));

        verify(rateLimiter).tryAcquire("rl:pwreset:email:test@example.com", 3, Duration.ofHours(1));
        verifyNoInteractions(userRepository, tokenGenerator, tokenHasher, eventPublisher);
        verify(passwordResetTokenRepository, never()).save(any());
    }

    @Test
    void shouldThrowWhenEmailIsInvalid() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(new RequestPasswordResetInput("invalid")));
    }
}
