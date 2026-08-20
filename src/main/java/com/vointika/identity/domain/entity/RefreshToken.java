package com.vointika.identity.domain.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

public class RefreshToken {

    /**
     * The 401 for a refresh token this server will not act on.
     *
     * <p><b>Four causes answer with it and must stay byte-identical.</b> In
     * {@code RefreshAccessTokenUseCase} they are: a hash matching no row (unknown or
     * forged), a row already revoked (<b>reuse — a probable theft, which also kills the
     * family</b>), a token whose user no longer exists, and the loser of a concurrent
     * rotation race. {@code LogoutUserUseCase} adds a fifth for an unknown token.
     *
     * <p><b>Unlike the tenant 404, no structure enforces this.</b>
     * {@code TourOperatorMembershipPolicy.ensureMember} throws once behind one
     * predicate, so its causes cannot differ whatever any string says; here there are
     * five separate {@code throw} statements, and only identical literals held them
     * together. A one-word edit to the reuse branch would tell an attacker that the
     * token they replayed was <em>recognised</em> — the single thing this endpoint must
     * not reveal, and the reason the family revocation happens silently.
     *
     * <p>Expiry is deliberately <em>not</em> folded in: {@code "Refresh token has
     * expired"} is a distinct, safe answer, because an expired token tells an attacker
     * only what the clock already tells them.
     *
     * <p>{@code RefreshTokenMessageIsWrittenOnceTest} fails the build if the sentence
     * reappears as a literal anywhere but here and its one pinning assertion.
     */
    public static final String INVALID = "Invalid refresh token";

    private static final int EXPIRATION_DAYS = 30;

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final UUID familyId;
    private UUID replacedById;
    private boolean revoked;
    private final Instant expiresAt;
    private final Instant createdAt;

    private RefreshToken(UUID id, UUID userId, String tokenHash, UUID familyId,
                         UUID replacedById, boolean revoked,
                         Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.familyId = familyId;
        this.replacedById = replacedById;
        this.revoked = revoked;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static RefreshToken newRoot(UUID id, UUID userId, String tokenHash) {
        Instant now = Instant.now();
        return new RefreshToken(id, userId, tokenHash, id, null, false,
                now.plus(Duration.ofDays(EXPIRATION_DAYS)), now);
    }

    /**
     * Builds the next token in {@code previous}'s family. Does NOT mutate
     * {@code previous}: revoking it (and stamping its {@code replaced_by_id}) is
     * done atomically in the DB via a guarded conditional update, so two
     * concurrent rotations of the same token can't both succeed — see
     * {@code RefreshTokenRepository.revokeForRotation}.
     */
    public static RefreshToken createRotation(RefreshToken previous, UUID newId, String newTokenHash) {
        Instant now = Instant.now();
        return new RefreshToken(newId, previous.userId, newTokenHash, previous.familyId, null, false,
                now.plus(Duration.ofDays(EXPIRATION_DAYS)), now);
    }

    public static RefreshToken rehydrate(UUID id, UUID userId, String tokenHash, UUID familyId,
                                         UUID replacedById, boolean revoked,
                                         Instant expiresAt, Instant createdAt) {
        return new RefreshToken(id, userId, tokenHash, familyId, replacedById, revoked, expiresAt, createdAt);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public UUID getFamilyId() { return familyId; }
    public UUID getReplacedById() { return replacedById; }
    public boolean isRevoked() { return revoked; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
