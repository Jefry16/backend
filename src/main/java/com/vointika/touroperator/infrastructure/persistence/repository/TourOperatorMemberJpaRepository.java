package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.enums.MemberRole;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorMemberJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourOperatorMemberJpaRepository extends JpaRepository<TourOperatorMemberJpaEntity, UUID> {
    boolean existsByUserId(UUID userId);

    List<TourOperatorMemberJpaEntity> findByUserId(UUID userId);

    boolean existsByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId);

    Optional<TourOperatorMemberJpaEntity> findByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId);

    List<TourOperatorMemberJpaEntity> findByTourOperatorId(UUID tourOperatorId);

    long countByTourOperatorIdAndRole(UUID tourOperatorId, MemberRole role);

    void deleteByTourOperatorIdAndUserId(UUID tourOperatorId, UUID userId);
}
