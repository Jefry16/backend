package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorMemberJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TourOperatorMemberJpaRepository extends JpaRepository<TourOperatorMemberJpaEntity, UUID> {
    boolean existsByUserId(UUID userId);
}
