package com.vointika.identity.infrastructure.persistence.repository;

import com.vointika.identity.infrastructure.persistence.entity.VerificationTokenJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VerificationTokenJpaRepository extends JpaRepository<VerificationTokenJpaEntity, UUID> {
    Optional<VerificationTokenJpaEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE VerificationTokenJpaEntity v SET v.status = 'EXPIRED' WHERE v.userId = :userId AND v.status = 'PENDING'")
    void expireAllPendingByUserId(@Param("userId") UUID userId);
}
