package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.ChangePasswordInput;
import com.vointika.shared.port.EventPublisherPort;
import com.vointika.identity.application.port.PasswordHasherPort;
import com.vointika.shared.event.PasswordChangedEvent;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.enums.UserStatus;
import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.exception.UnauthorizedException;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordHasherPort passwordHasher;
    @Mock private EventPublisherPort eventPublisher;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public <T> T call(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void run(Runnable work) { work.run(); }
    };

    private ChangePasswordUseCase useCase;
    private UUID userId;

    @BeforeEach
    void setUp() {
        // requireById is a default method: Mockito would stub it to null and every
        // "invalid principal" assertion below would pass without running the branch.
        // lenient() because not every test in this class reaches the lookup.
        lenient().doCallRealMethod().when(userRepository).requireById(any());
        useCase = new ChangePasswordUseCase(userRepository, refreshTokenRepository, passwordHasher, eventPublisher, transactionRunner);
        userId = UUID.randomUUID();
    }

    @Test
    void shouldChangePasswordSuccessfully() {
        User user = verifiedUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("OldPass1!", "hashed")).thenReturn(true);
        when(passwordHasher.matches("NewPass2@", "hashed")).thenReturn(false);
        when(passwordHasher.hash("NewPass2@")).thenReturn("new-hashed");

        useCase.execute(new ChangePasswordInput(userId, "OldPass1!", "NewPass2@"));

        verify(userRepository).save(user);
        verify(refreshTokenRepository).revokeAllByUserId(userId);
        verify(eventPublisher).publish(any(PasswordChangedEvent.class));
        assertEquals("new-hashed", user.getHashedPassword());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new ChangePasswordInput(userId, "OldPass1!", "NewPass2@")));
    }

    @Test
    void shouldThrowWhenCurrentPasswordIsWrong() {
        User user = verifiedUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("WrongPass1!", "hashed")).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new ChangePasswordInput(userId, "WrongPass1!", "NewPass2@")));
        assertEquals("Current password is incorrect", ex.getMessage());
    }

    @Test
    void shouldThrowWhenNewPasswordSameAsCurrent() {
        User user = verifiedUser();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(passwordHasher.matches("SamePass1!", "hashed")).thenReturn(true);
        when(passwordHasher.matches("SamePass1!", "hashed")).thenReturn(true);

        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(new ChangePasswordInput(userId, "SamePass1!", "SamePass1!")));
    }

    @Test
    void shouldThrowWhenNewPasswordIsWeak() {
        assertThrows(InvalidFieldException.class,
                () -> useCase.execute(new ChangePasswordInput(userId, "OldPass1!", "weak")));
    }

    private User verifiedUser() {
        return new User(userId, new Email("test@example.com"), new UserName("Test"),
                "hashed", UserStatus.VERIFIED, null, "en", Instant.now(), Instant.now());
    }
}
