package com.vointika.audit.infrastructure.persistence.repository;

import com.vointika.audit.infrastructure.persistence.entity.AuditLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AuditLogJpaRepository extends JpaRepository<AuditLogJpaEntity, UUID> {

    Optional<AuditLogJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);
}
