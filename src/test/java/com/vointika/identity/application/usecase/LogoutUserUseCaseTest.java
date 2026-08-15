package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.LogoutUserInput;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.identity.domain.entity.RefreshToken;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
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
class LogoutUserUseCaseTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private TokenHasherPort tokenHasher;

    private LogoutUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new LogoutUserUseCase(refreshTokenRepository, tokenHasher);
    }

    @Test
    void logoutRevokesEntireFamilyForOwningUser() {
        UUID userId = UUID.randomUUID();
        RefreshToken token = RefreshToken.newRoot(UUID.randomUUID(), userId, "hash");
        when(tokenHasher.hash("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        useCase.execute(new LogoutUserInput(userId, "raw"));

        verify(refreshTokenRepository).revokeAllByFamilyId(token.getFamilyId());
    }

    @Test
    void shouldThrowWhenTokenNotFound() {
        when(tokenHasher.hash("bad")).thenReturn("bad-hash");
        when(refreshTokenRepository.findByTokenHash("bad-hash")).thenReturn(Optional.empty());

        UnauthorizedException ex = assertThrows(UnauthorizedException.class,
                () -> useCase.execute(new LogoutUserInput(UUID.randomUUID(), "bad")));
        assertEquals("Invalid refresh token", ex.getMessage());
    }

    @Test
    void shouldThrowWhenRefreshTokenBelongsToDifferentUser() {
        UUID tokenOwner = UUID.randomUUID();
        UUID differentUser = UUID.randomUUID();
        RefreshToken token = RefreshToken.newRoot(UUID.randomUUID(), tokenOwner, "hash");
        when(tokenHasher.hash("raw")).thenReturn("hash");
        when(refreshTokenRepository.findByTokenHash("hash")).thenReturn(Optional.of(token));

        assertThrows(ForbiddenException.class,
                () -> useCase.execute(new LogoutUserInput(differentUser, "raw")));
        verify(refreshTokenRepository, never()).revokeAllByFamilyId(any());
    }

}
