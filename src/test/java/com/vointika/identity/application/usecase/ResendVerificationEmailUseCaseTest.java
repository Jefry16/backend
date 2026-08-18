package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.ResendVerificationEmailInput;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.shared.event.VerificationEmailRequestedEvent;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.entity.VerificationToken;
import com.vointika.identity.domain.enums.UserStatus;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.repository.VerificationTokenRepository;
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
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResendVerificationEmailUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private TokenGeneratorPort tokenGenerator;
    @Mock private TokenHasherPort tokenHasher;
    @Mock private EventPublisherPort eventPublisher;
    @Mock private RateLimiterPort rateLimiter;

    private final IdGenerator idGenerator = UUID::randomUUID;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public <T> T call(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void run(Runnable work) { work.run(); }
    };

    private ResendVerificationEmailUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResendVerificationEmailUseCase(userRepository, verificationTokenRepository,
                tokenGenerator, tokenHasher, eventPublisher, transactionRunner, idGenerator, rateLimiter);
    }

    @Test
    void resendPersistsHashedTokenAndPublishesRawTokenInEvent() {
        User user = new User(UUID.randomUUID(), new Email("test@example.com"), new UserName("Test"), "hashed");
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(tokenGenerator.generateVerificationToken()).thenReturn("raw-new");
        when(tokenHasher.hash("raw-new")).thenReturn("hashed-new");

        useCase.execute(new ResendVerificationEmailInput("test@example.com"));

        verify(verificationTokenRepository).expireAllByUserId(user.getId());

        ArgumentCaptor<VerificationToken> savedToken = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository).save(savedToken.capture());
        assertEquals("hashed-new", savedToken.getValue().getTokenHash());

        ArgumentCaptor<VerificationEmailRequestedEvent> publishedEvent =
                ArgumentCaptor.forClass(VerificationEmailRequestedEvent.class);
        verify(eventPublisher).publish(publishedEvent.capture());
        assertEquals("raw-new", publishedEvent.getValue().token());
    }

    @Test
    void shouldSilentlyNoOpWhenUserNotFound() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());

        assertDoesNotThrow(
                () -> useCase.execute(new ResendVerificationEmailInput("test@example.com")));

        // Timing equalization (§7.5): token work runs, nothing persists/sends.
        verify(tokenGenerator).generateVerificationToken();
        verify(verificationTokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldSilentlyNoOpWhenUserAlreadyVerified() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, new Email("test@example.com"), new UserName("Test"),
                "hashed", UserStatus.VERIFIED, null, "en", Instant.now(), Instant.now());
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));

        assertDoesNotThrow(
                () -> useCase.execute(new ResendVerificationEmailInput("test@example.com")));

        verify(tokenGenerator).generateVerificationToken();
        verify(verificationTokenRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldSilentlyNoOpWhenRateLimited() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(false);

        assertDoesNotThrow(
                () -> useCase.execute(new ResendVerificationEmailInput("test@example.com")));

        verify(rateLimiter).tryAcquire("rl:resend:email:test@example.com", 3, Duration.ofHours(1));
        verifyNoInteractions(userRepository, tokenGenerator, tokenHasher, eventPublisher);
        verify(verificationTokenRepository, never()).save(any());
    }
}
