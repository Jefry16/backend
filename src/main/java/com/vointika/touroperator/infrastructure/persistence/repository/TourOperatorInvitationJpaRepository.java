package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.enums.InvitationStatus;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorInvitationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TourOperatorInvitationJpaRepository
        extends JpaRepository<TourOperatorInvitationJpaEntity, UUID> {

    Optional<TourOperatorInvitationJpaEntity> findByTokenHash(String tokenHash);

    boolean existsByTourOperatorIdAndEmailAndStatus(UUID tourOperatorId, String email, InvitationStatus status);
}
