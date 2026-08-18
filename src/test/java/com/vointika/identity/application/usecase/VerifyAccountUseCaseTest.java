package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.VerifyAccountInput;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.entity.VerificationToken;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.repository.VerificationTokenRepository;
import com.vointika.shared.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.shared.port.TransactionRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerifyAccountUseCaseTest {

    @Mock private VerificationTokenRepository verificationTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private TokenHasherPort tokenHasher;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public <T> T call(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void run(Runnable work) { work.run(); }
    };

    private VerifyAccountUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new VerifyAccountUseCase(verificationTokenRepository, userRepository, tokenHasher, transactionRunner);
    }

    @Test
    void verifyHashesIncomingTokenAndMarksUserVerified() {
        UUID userId = UUID.randomUUID();
        VerificationToken token = VerificationToken.issue(UUID.randomUUID(), userId, "hashed-token");
        User user = new User(UUID.randomUUID(), new Email("test@example.com"), new UserName("Test"), "hashed");

        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        when(verificationTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        useCase.execute(new VerifyAccountInput("raw-token"));

        verify(verificationTokenRepository).save(token);
        verify(userRepository).save(user);
        assertTrue(user.isVerified());
    }

    @Test
    void shouldThrowWhenTokenNotFound() {
        when(tokenHasher.hash("bad-token")).thenReturn("bad-hash");
        when(verificationTokenRepository.findByTokenHash("bad-hash")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new VerifyAccountInput("bad-token")));
        assertEquals("Invalid verification token", ex.getMessage());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        VerificationToken token = VerificationToken.issue(UUID.randomUUID(), userId, "hashed-token");

        when(tokenHasher.hash("raw-token")).thenReturn("hashed-token");
        when(verificationTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(token));
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new VerifyAccountInput("raw-token")));
    }
}
