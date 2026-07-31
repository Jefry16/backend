package com.vointika.identity.application.usecase;

import com.vointika.shared.port.DiagnosticLogPort;
import com.vointika.identity.application.dto.input.RefreshAccessTokenInput;
import com.vointika.identity.application.dto.output.RefreshAccessTokenOutput;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshAccessTokenUseCaseTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private TokenGeneratorPort tokenGenerator;
    @Mock private TokenHasherPort tokenHasher;
    @Mock private DiagnosticLogPort diagnosticLog;

    private final IdGenerator idGenerator = UUID::randomUUID;

    private final TransactionRunner transactionRunner = new TransactionRunner() {
        @Override public <T> T call(java.util.function.Supplier<T> work) { return work.get(); }
        @Override public void run(Runnable work) { work.run(); }
    };

    private RefreshAccessTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RefreshAccessTokenUseCase(refreshTokenRepository, userRepository,
                tokenGenerator, tokenHasher, transactionRunner, idGenerator, diagnosticLog);
    }

    @Test
    void rotatesTokenAndIssuesNewAccessToken() {
        UUID userId = UUID.randomUUID();
        RefreshToken existing = RefreshToken.newRoot(UUID.randomUUID(), userId, "old-hash");
        User user = verifiedUser(userId);

        when(tokenHasher.hash("raw-old")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenGenerator.generateRefreshToken()).thenReturn("raw-new");
        when(tokenHasher.hash("raw-new")).thenReturn("new-hash");
        when(tokenGenerator.generateAccessToken(userId.toString())).thenReturn("new-access-token");
        when(refreshTokenRepository.revokeForRotation(eq(existing.getId()), any(UUID.class))).thenReturn(true);

        RefreshAccessTokenOutput output = useCase.execute(new RefreshAccessTokenInput("raw-old"));

        assertEquals("new-access-token", output.accessToken());
        assertEquals("raw-new", output.refreshToken());

        // Only the rotated child is saved; the previous token is consumed atomically
        // via the guarded conditional update (not a full-row save).
        ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(saved.capture());
        RefreshToken rotated = saved.getValue();
        assertEquals(existing.getFamilyId(), rotated.getFamilyId(), "rotated keeps family");
        assertEquals("new-hash", rotated.getTokenHash());
        assertFalse(rotated.isRevoked());

        // The previous token was revoked-for-rotation, stamped with the new child's id.
        ArgumentCaptor<UUID> replacedBy = ArgumentCaptor.forClass(UUID.class);
        verify(refreshTokenRepository).revokeForRotation(eq(existing.getId()), replacedBy.capture());
        assertEquals(rotated.getId(), replacedBy.getValue());
    }

    @Test
    void concurrentRotationLosesRaceAndIsRejectedWithoutFamilyRevocation() {
        UUID userId = UUID.randomUUID();
        RefreshToken existing = RefreshToken.newRoot(UUID.randomUUID(), userId, "old-hash");
        User user = verifiedUser(userId);

        when(tokenHasher.hash("raw-old")).thenReturn("old-hash");
        when(refreshTokenRepository.findByTokenHash("old-hash")).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(tokenGenerator.generateRefreshToken()).thenReturn("raw-new");
        when(tokenHasher.hash("raw-new")).thenReturn("new-hash");
        // A concurrent rotation already consumed this token: the guarded update hits 0 rows.
        when(refreshTokenRepository.revokeForRotation(eq(existing.getId()), any(UUID.class))).thenReturn(false);

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new RefreshAccessTokenInput("raw-old")));
        assertEquals("Invalid refresh token", ex.getMessage());

        verify(refreshTokenRepository, never()).save(any());                 // no child minted
        verify(refreshTokenRepository, never()).revokeAllByFamilyId(any());  // family NOT nuked
    }

    @Test
    void shouldThrowWhenTokenNotFound() {
        when(tokenHasher.hash("bad-token")).thenReturn("bad-hash");
        when(refreshTokenRepository.findByTokenHash("bad-hash")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new RefreshAccessTokenInput("bad-token")));
        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void reusedRevokedTokenTriggersFamilyRevocationAndUnauthorized() {
        UUID userId = UUID.randomUUID();
        UUID tokenId = UUID.randomUUID();
        // An already-revoked (but unexpired) root token, as the DB would hand back
        // on a reuse attempt. Built via rehydrate since revoke() is not a domain op.
        RefreshToken revoked = RefreshToken.rehydrate(
                tokenId, userId, "stolen-hash", tokenId, null, true,
                Instant.now().plus(Duration.ofDays(30)), Instant.now());

        when(tokenHasher.hash("stolen-raw")).thenReturn("stolen-hash");
        when(refreshTokenRepository.findByTokenHash("stolen-hash")).thenReturn(Optional.of(revoked));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new RefreshAccessTokenInput("stolen-raw")));
        assertEquals("Invalid refresh token", ex.getMessage());

        verify(refreshTokenRepository).revokeAllByFamilyId(revoked.getFamilyId());
        verify(refreshTokenRepository, never()).save(any());
        verifyNoInteractions(userRepository);
    }

    @Test
    void shouldThrowWhenTokenExpired() {
        UUID userId = UUID.randomUUID();
        RefreshToken expired = RefreshToken.rehydrate(
                UUID.randomUUID(), userId, "expired-hash",
                UUID.randomUUID(), null, false,
                Instant.now().minus(Duration.ofHours(1)),
                Instant.now().minus(Duration.ofDays(31)));

        when(tokenHasher.hash("raw-expired")).thenReturn("expired-hash");
        when(refreshTokenRepository.findByTokenHash("expired-hash")).thenReturn(Optional.of(expired));

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new RefreshAccessTokenInput("raw-expired")));
        assertEquals("Refresh token has expired", ex.getMessage());
        verify(refreshTokenRepository, never()).revokeAllByFamilyId(any());
    }

    @Test
    void shouldThrowWhenUserNotVerified() {
        UUID userId = UUID.randomUUID();
        RefreshToken existing = RefreshToken.newRoot(UUID.randomUUID(), userId, "h");
        User unverified = new User(userId, new Email("test@example.com"), new UserName("Test"), "hashed");

        when(tokenHasher.hash("raw")).thenReturn("h");
        when(refreshTokenRepository.findByTokenHash("h")).thenReturn(Optional.of(existing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(unverified));

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> useCase.execute(new RefreshAccessTokenInput("raw")));
        assertEquals("Account is not verified", ex.getMessage());
    }

    private User verifiedUser(UUID id) {
        return new User(id, new Email("test@example.com"), new UserName("Test"),
                "hashed", UserStatus.VERIFIED, null, "en", Instant.now(), Instant.now());
    }
}
