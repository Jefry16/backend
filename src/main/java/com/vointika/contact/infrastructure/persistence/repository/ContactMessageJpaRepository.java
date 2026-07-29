package com.vointika.contact.infrastructure.persistence.repository;

import com.vointika.contact.infrastructure.persistence.entity.ContactMessageJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContactMessageJpaRepository extends JpaRepository<ContactMessageJpaEntity, UUID> {

    Optional<ContactMessageJpaEntity> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);
}
