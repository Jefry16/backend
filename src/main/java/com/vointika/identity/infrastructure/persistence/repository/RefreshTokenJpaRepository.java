package com.vointika.identity.infrastructure.persistence.repository;

import com.vointika.identity.infrastructure.persistence.entity.RefreshTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenJpaEntity, UUID> {
    Optional<RefreshTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revoked = true WHERE r.familyId = :familyId AND r.revoked = false")
    void revokeAllByFamilyId(@Param("familyId") UUID familyId);

    /**
     * Atomically consume a token for rotation: revoke it and stamp its
     * replacement, but only if it is still live. The {@code AND r.revoked = false}
     * guard plus the row lock make this the race winner — a concurrent rotation
     * of the same token re-evaluates against the committed row and matches 0 rows.
     *
     * @return rows updated: 1 if this caller won, 0 if the token was already revoked.
     */
    @Modifying
    @Query("UPDATE RefreshTokenJpaEntity r SET r.revoked = true, r.replacedById = :replacedById "
            + "WHERE r.id = :id AND r.revoked = false")
    int revokeForRotation(@Param("id") UUID id, @Param("replacedById") UUID replacedById);
}
