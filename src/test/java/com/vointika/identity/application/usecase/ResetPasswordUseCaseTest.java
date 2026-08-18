package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.ResetPasswordInput;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.identity.application.port.PasswordHasherPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.shared.event.PasswordChangedEvent;
import com.vointika.identity.domain.entity.PasswordResetToken;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.enums.UserStatus;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.identity.domain.repository.PasswordResetTokenRepository;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.shared.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.shared.port.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResetPasswordUseCaseTest {

    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordHasherPort passwordHasher;
    @Mock private TokenHasherPort tokenHasher;
    @Mock private EventPublisherPort eventPublisher;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public <T> T call(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void run(Runnable work) { work.run(); }
    };

    private ResetPasswordUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResetPasswordUseCase(passwordResetTokenRepository, userRepository,
                refreshTokenRepository, passwordHasher, tokenHasher, eventPublisher, transactionRunner);
    }

    @Test
    void shouldResetPasswordSuccessfully() {
        UUID userId = UUID.randomUUID();
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), userId, "hashed-reset");
        User user = verifiedUser(userId);

        when(tokenHasher.hash("raw-reset")).thenReturn("hashed-reset");
        when(passwordResetTokenRepository.findByTokenHash("hashed-reset")).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("NewPass2@", "hashed")).thenReturn(false);
        when(passwordHasher.hash("NewPass2@")).thenReturn("new-hashed");

        useCase.execute(new ResetPasswordInput("raw-reset", "NewPass2@"));

        verify(passwordResetTokenRepository).save(token);
        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(userId);
        verify(eventPublisher).publish(any(PasswordChangedEvent.class));
        assertEquals("new-hashed", user.getHashedPassword());
    }

    @Test
    void shouldThrowWhenTokenNotFound() {
        when(tokenHasher.hash("bad-token")).thenReturn("bad-hash");
        when(passwordResetTokenRepository.findByTokenHash("bad-hash")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new ResetPasswordInput("bad-token", "NewPass2@")));
        assertEquals("Invalid password reset token", ex.getMessage());
    }

    @Test
    void shouldThrowWhenNewPasswordSameAsCurrent() {
        UUID userId = UUID.randomUUID();
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), userId, "hashed-reset");
        User user = verifiedUser(userId);

        when(tokenHasher.hash("raw-reset")).thenReturn("hashed-reset");
        when(passwordResetTokenRepository.findByTokenHash("hashed-reset")).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("SamePass1!", "hashed")).thenReturn(true);

        InvalidFieldException ex = assertThrows(InvalidFieldException.class,
                () -> useCase.execute(new ResetPasswordInput("raw-reset", "SamePass1!")));
        assertEquals("New password must be different from current password", ex.getMessage());
    }

    private User verifiedUser(UUID id) {
        return new User(id, new Email("test@example.com"), new UserName("Test"),
                "hashed", UserStatus.VERIFIED, null, "en", Instant.now(), Instant.now());
    }
}
