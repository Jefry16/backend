package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.LoginUserInput;
import com.vointika.identity.application.dto.output.LoginUserOutput;
import com.vointika.identity.application.port.PasswordHasherPort;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.identity.domain.entity.RefreshToken;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.enums.UserStatus;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.identity.domain.valueobject.Email;
import com.vointika.identity.domain.valueobject.UserName;
import com.vointika.shared.port.RateLimiterPort;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginUserUseCaseTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordHasherPort passwordHasher;
    @Mock private TokenGeneratorPort tokenGenerator;
    @Mock private TokenHasherPort tokenHasher;
    @Mock private RateLimiterPort rateLimiter;

    private final IdGenerator idGenerator = UUID::randomUUID;

    private LoginUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LoginUserUseCase(userRepository, refreshTokenRepository,
                passwordHasher, tokenGenerator, tokenHasher, idGenerator, rateLimiter);
    }

    @Test
    void loginPersistsHashedRootTokenAndReturnsRawTokenInOutput() {
        User user = verifiedUser();
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("Password1!", "hashed")).thenReturn(true);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);
        when(tokenGenerator.generateRefreshToken()).thenReturn("raw-refresh");
        when(tokenHasher.hash("raw-refresh")).thenReturn("hashed-refresh");
        when(tokenGenerator.generateAccessToken(user.getId().toString())).thenReturn("access-token");

        LoginUserOutput output = useCase.execute(new LoginUserInput("test@example.com", "Password1!"));

        assertEquals("access-token", output.accessToken());
        assertEquals("raw-refresh", output.refreshToken());

        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        RefreshToken persisted = saved.getValue();
        assertEquals("hashed-refresh", persisted.getTokenHash());
        assertEquals(user.getId(), persisted.getUserId());
        assertEquals(persisted.getId(), persisted.getFamilyId(), "root token's familyId == its id");
        assertFalse(persisted.isRevoked());
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.empty());
        when(passwordHasher.matches(eq("Password1!"), any(String.class))).thenReturn(false);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new LoginUserInput("test@example.com", "Password1!")));
        assertEquals("Invalid credentials", ex.getMessage());
        verify(passwordHasher).matches(eq("Password1!"), any(String.class));
    }

    @Test
    void shouldThrowWhenPasswordDoesNotMatch() {
        User user = verifiedUser();
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("WrongPass1!", "hashed")).thenReturn(false);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new LoginUserInput("test@example.com", "WrongPass1!")));
        assertEquals("Invalid credentials", ex.getMessage());
    }

    @Test
    void shouldThrowWhenAccountNotVerified() {
        User user = new User(UUID.randomUUID(), new Email("test@example.com"), new UserName("Test"), "hashed");
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("Password1!", "hashed")).thenReturn(true);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(true);

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> useCase.execute(new LoginUserInput("test@example.com", "Password1!")));
        assertEquals("Account is not verified", ex.getMessage());
    }

    @Test
    void shouldThrowInvalidCredentialsWhenRateLimitedEvenWithCorrectPassword() {
        User user = verifiedUser();
        when(userRepository.findByEmail(any(Email.class))).thenReturn(Optional.of(user));
        when(passwordHasher.matches("Password1!", "hashed")).thenReturn(true);
        when(rateLimiter.tryAcquire(anyString(), anyInt(), any(Duration.class))).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new LoginUserInput("test@example.com", "Password1!")));
        assertEquals("Invalid credentials", ex.getMessage(),
                "throttle denial must be indistinguishable from a wrong password");

        // BCrypt ran BEFORE the throttle check (timing parity), and the limiter
        // was consulted with the per-email key
        verify(passwordHasher).matches("Password1!", "hashed");
        verify(rateLimiter).tryAcquire("rl:login:email:test@example.com", 20, Duration.ofMinutes(15));
        verify(refreshTokenRepository, never()).save(any());
    }

    private User verifiedUser() {
        return new User(UUID.randomUUID(), new Email("test@example.com"), new UserName("Test"),
                "hashed", UserStatus.VERIFIED, null, "en", Instant.now(), Instant.now());
    }
}
