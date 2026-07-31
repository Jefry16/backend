package com.vointika.identity.infrastructure.persistence.repository;

import com.vointika.identity.infrastructure.persistence.entity.UserJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, UUID> {
    Optional<UserJpaEntity> findByEmail(String email);

    /** Batch account fetch by id — one-query resolution for the member list, audit actors, and alert recipients (no N+1). */
}