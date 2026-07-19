package com.vointika.identity.application.usecase;

import com.vointika.identity.application.dto.input.RefreshAccessTokenInput;
import com.vointika.identity.application.dto.output.RefreshAccessTokenOutput;
import com.vointika.identity.application.port.TokenGeneratorPort;
import com.vointika.identity.application.port.TokenHasherPort;
import com.vointika.identity.domain.entity.RefreshToken;
import com.vointika.identity.domain.entity.User;
import com.vointika.identity.domain.repository.RefreshTokenRepository;
import com.vointika.identity.domain.repository.UserRepository;
import com.vointika.shared.exception.ForbiddenException;
import com.vointika.shared.exception.UnauthorizedException;
import com.vointika.shared.port.TransactionRunner;
import com.vointika.shared.service.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class RefreshAccessTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(RefreshAccessTokenUseCase.class);

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final TokenGeneratorPort tokenGenerator;
    private final TokenHasherPort tokenHasher;
    private final TransactionRunner transactionRunner;
    private final IdGenerator idGenerator;

    public RefreshAccessTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository,
            TokenGeneratorPort tokenGenerator,
            TokenHasherPort tokenHasher,
            TransactionRunner transactionRunner,
            IdGenerator idGenerator
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.tokenGenerator = tokenGenerator;
        this.tokenHasher = tokenHasher;
        this.transactionRunner = transactionRunner;
        this.idGenerator = idGenerator;
    }

    public RefreshAccessTokenOutput execute(RefreshAccessTokenInput input) {
        String presentedHash = tokenHasher.hash(input.refreshToken());

        RefreshToken presented = refreshTokenRepository
                .findByTokenHash(presentedHash)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // Reuse detection: a revoked-but-existing token presented again is a strong
        // theft signal — kill the entire family so the attacker (and any legitimate
        // session along the same chain) is forced back through login.
        if (presented.isRevoked()) {
            log.warn("Refresh token reuse detected; revoking family. userId={} familyId={}",
                    presented.getUserId(), presented.getFamilyId());
            refreshTokenRepository.revokeAllByFamilyId(presented.getFamilyId());
            throw new UnauthorizedException("Invalid refresh token");
        }

        if (Instant.now().isAfter(presented.getExpiresAt())) {
            throw new UnauthorizedException("Refresh token has expired");
        }

        User user = userRepository
                .findById(presented.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        // 403 (not 401), consistent with login: the refresh token authenticated
        // the caller, so an unverified account is authenticated-but-forbidden.
        // Defensive — unreachable in practice, since a refresh token only exists
        // after a successful (verified) login and there is no un-verify path.
        if (!user.isVerified()) {
            throw new ForbiddenException("Account is not verified");
        }

        String newRawRefreshToken = tokenGenerator.generateRefreshToken();
        java.util.UUID newId = idGenerator.newId();
        RefreshToken rotated = RefreshToken.createRotation(
                presented, newId, tokenHasher.hash(newRawRefreshToken));

        transactionRunner.run(() -> {
            // Consume the presented token atomically: revoke it only if still
            // live, then mint its single child. Two concurrent rotations of the
            // same token race on this guarded update — the loser sees 0 rows and
            // is rejected, so a token can never spawn two live siblings. (Plain
            // 401, not reuse-detection: a concurrent double-submit of a *valid*
            // token must not nuke the whole family.)
            if (!refreshTokenRepository.revokeForRotation(presented.getId(), newId)) {
                throw new UnauthorizedException("Invalid refresh token");
            }
            refreshTokenRepository.save(rotated);
        });

        String accessToken = tokenGenerator.generateAccessToken(user.getId().toString());
        return new RefreshAccessTokenOutput(accessToken, newRawRefreshToken);
    }
}
