package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorBrandJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TourOperatorBrandJpaRepository extends JpaRepository<TourOperatorBrandJpaEntity, UUID> {
}
