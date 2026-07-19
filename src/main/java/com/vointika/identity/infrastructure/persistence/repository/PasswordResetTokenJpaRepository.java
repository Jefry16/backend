package com.vointika.identity.infrastructure.persistence.repository;

import com.vointika.identity.infrastructure.persistence.entity.PasswordResetTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetTokenJpaEntity, UUID> {
    Optional<PasswordResetTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE PasswordResetTokenJpaEntity p SET p.status = 'EXPIRED' WHERE p.userId = :userId AND p.status = 'PENDING'")
    void expireAllPendingByUserId(@Param("userId") UUID userId);
}
