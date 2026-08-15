package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.LogoutUserInput;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.identity.domain.entity.RefreshToken;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.UnauthorizedException;

import java.util.UUID;

public class LogoutUserUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHasherPort tokenHasher;

    public LogoutUserUseCase(RefreshTokenRepository refreshTokenRepository, TokenHasherPort tokenHasher) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHasher = tokenHasher;
    }

    public void execute(LogoutUserInput input) {
        UUID userId = input.userId();

        RefreshToken refreshToken = refreshTokenRepository
                .findByTokenHash(tokenHasher.hash(input.refreshToken()))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!refreshToken.getUserId().equals(userId)) {
            throw new ForbiddenException("Refresh token does not belong to the authenticated user");
        }

        // Logout means "this device is done" — kill the whole rotation chain.
        refreshTokenRepository.revokeAllByFamilyId(refreshToken.getFamilyId());
    }
}
