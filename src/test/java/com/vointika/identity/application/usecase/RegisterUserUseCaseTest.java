package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.RegisterUserInput;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.identity.application.port.PasswordHasherPort;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.shared.event.AccountAlreadyRegisteredEvent;
import com.vointika.shared.event.VerificationEmailRequestedEvent;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.entity.VerificationToken;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.repository.VerificationTokenRepository;
import com.vointika.identity.domain.valueobject.Email;
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
import com.vointika.shared.exception.UniqueConstraintViolationException;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegisterUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private PasswordHasherPort passwordHasher;
    @Mock private TokenGeneratorPort tokenGenerator;
    @Mock private TokenHasherPort tokenHasher;
    @Mock private EventPublisherPort eventPublisher;
    @Mock private RateLimiterPort rateLimiter;

    private final IdGenerator idGenerator = UUID::randomUUID;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public <T> T call(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void run(Runnable work) { work.run(); }
    };

    private RegisterUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterUserUseCase(userRepository, verificationTokenRepository,
                passwordHasher, tokenGenerator, tokenHasher, eventPublisher, transactionRunner, idGenerator,
                rateLimiter, java.util.Set.of("en", "es"));
    }

    private void allowNotification() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
    }

    private void exhaustNotificationCooldown() {
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(false);
    }

    @Test
    void registerPersistsHashedTokenAndPublishesRawTokenInEvent() {
        allowNotification();
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(passwordHasher.hash("Password1!")).thenReturn("hashed");
        when(tokenGenerator.generateVerificationToken()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");

        useCase.execute(new RegisterUserInput("test@example.com", "Test User", "Password1!", null));

        verify(userRepository).save(any(User.class));

        ArgumentCaptor<VerificationToken> savedToken = ArgumentCaptor.forClass(VerificationToken.class);
        verify(verificationTokenRepository).save(savedToken.capture());
        assertEquals("hashed-token", savedToken.getValue().getTokenHash());

        ArgumentCaptor<VerificationEmailRequestedEvent> publishedEvent =
                ArgumentCaptor.forClass(VerificationEmailRequestedEvent.class);
        verify(eventPublisher).publish(publishedEvent.capture());
        assertEquals("raw-token", publishedEvent.getValue().token());
    }

    @Test
    void existingEmailPublishesAccountAlreadyRegisteredEventAndSavesNothing() {
        allowNotification();
        User existing = new User(UUID.randomUUID(), new Email("test@example.com"),
                new UserName("Existing User"), "existing-hash");
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existing));
        when(passwordHasher.hash("Password1!")).thenReturn("hashed");

        assertDoesNotThrow(
                () -> useCase.execute(new RegisterUserInput("test@example.com", "Attacker Name", "Password1!", null)));

        // Timing parity (§7.5): the hash runs even when the email is taken
        verify(passwordHasher).hash("Password1!");
        verify(userRepository, never()).save(any());
        verify(verificationTokenRepository, never()).save(any());

        ArgumentCaptor<AccountAlreadyRegisteredEvent> publishedEvent =
                ArgumentCaptor.forClass(AccountAlreadyRegisteredEvent.class);
        verify(eventPublisher).publish(publishedEvent.capture());
        assertEquals("test@example.com", publishedEvent.getValue().email());
        assertEquals("Existing User", publishedEvent.getValue().name(),
                "event carries the EXISTING user's name, not the registrant's input");
    }

    @Test
    void duplicateEmailRaceAtCommitPublishesEventAndReturnsNormally() {
        allowNotification();
        User winner = new User(UUID.randomUUID(), new Email("test@example.com"),
                new UserName("Existing User"), "existing-hash");
        // Fast path sees no user; after the unique-constraint violation the winner is looked up
        when(userRepository.findByEmail(any(Email.class)))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(winner));
        when(passwordHasher.hash("Password1!")).thenReturn("hashed");
        when(tokenGenerator.generateVerificationToken()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        doThrow(new UniqueConstraintViolationException("duplicate key", null))
                .when(userRepository).save(any(User.class));

        assertDoesNotThrow(
                () -> useCase.execute(new RegisterUserInput("test@example.com", "Test User", "Password1!", null)));

        verify(verificationTokenRepository, never()).save(any());

        ArgumentCaptor<AccountAlreadyRegisteredEvent> publishedEvent =
                ArgumentCaptor.forClass(AccountAlreadyRegisteredEvent.class);
        verify(eventPublisher).publish(publishedEvent.capture());
        verifyNoMoreInteractions(eventPublisher);
        assertEquals("test@example.com", publishedEvent.getValue().email());
        assertEquals("Existing User", publishedEvent.getValue().name());
    }

    @Test
    void existingEmailOverCooldownPublishesNothingAndStillReturnsNormally() {
        exhaustNotificationCooldown();
        User existing = new User(UUID.randomUUID(), new Email("test@example.com"),
                new UserName("Existing User"), "existing-hash");
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(existing));
        when(passwordHasher.hash("Password1!")).thenReturn("hashed");

        assertDoesNotThrow(
                () -> useCase.execute(new RegisterUserInput("test@example.com", "Attacker Name", "Password1!", null)));

        verify(rateLimiter).tryAcquire("rl:register:email:test@example.com", 3, Duration.ofHours(1));
        verifyNoInteractions(eventPublisher);
        verify(userRepository, never()).save(any());
    }

    @Test
    void freshRegistrationOverCooldownStillRegistersAndSendsVerificationEmail() {
        // The cooldown gates only the already-registered notice — a real new
        // registration must never be blocked by it.
        exhaustNotificationCooldown();
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(passwordHasher.hash("Password1!")).thenReturn("hashed");
        when(tokenGenerator.generateVerificationToken()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");

        useCase.execute(new RegisterUserInput("test@example.com", "Test User", "Password1!", null));

        verify(userRepository).save(any(User.class));
        verify(eventPublisher).publish(any(VerificationEmailRequestedEvent.class));
    }

    @Test
    void duplicateEmailRaceOverCooldownPublishesNothing() {
        exhaustNotificationCooldown();
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(passwordHasher.hash("Password1!")).thenReturn("hashed");
        when(tokenGenerator.generateVerificationToken()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        doThrow(new UniqueConstraintViolationException("duplicate key", null))
                .when(userRepository).save(any(User.class));

        assertDoesNotThrow(
                () -> useCase.execute(new RegisterUserInput("test@example.com", "Test User", "Password1!", null)));

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void shouldThrowWhenEmailIsInvalid() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(new RegisterUserInput("invalid", "Test User", "Password1!", null)));
    }

    @Test
    void shouldThrowWhenPasswordIsWeak() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(new RegisterUserInput("test@example.com", "Test User", "weak", null)));
    }

    @Test
    void shouldThrowWhenNameIsBlank() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(new RegisterUserInput("test@example.com", "", "Password1!", null)));
    }

    @Test
    void capturesSupportedLanguageOntoUserAndVerificationEvent() {
        allowNotification();
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(passwordHasher.hash("Password1!")).thenReturn("hashed");
        when(tokenGenerator.generateVerificationToken()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");

        useCase.execute(new RegisterUserInput("test@example.com", "Test User", "Password1!", "es"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("es", savedUser.getValue().getLanguage());

        ArgumentCaptor<VerificationEmailRequestedEvent> event =
                ArgumentCaptor.forClass(VerificationEmailRequestedEvent.class);
        verify(eventPublisher).publish(event.capture());
        assertEquals("es", event.getValue().locale());
    }

    @Test
    void unsupportedOrAbsentLanguageDefaultsToEn() {
        allowNotification();
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(passwordHasher.hash("Password1!")).thenReturn("hashed");
        when(tokenGenerator.generateVerificationToken()).thenReturn("raw-token");
        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");

        useCase.execute(new RegisterUserInput("test@example.com", "Test User", "Password1!", "de"));

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertEquals("en", savedUser.getValue().getLanguage());
    }
}
