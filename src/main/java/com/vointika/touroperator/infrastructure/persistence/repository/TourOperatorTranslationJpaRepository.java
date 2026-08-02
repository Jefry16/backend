package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorTranslationId;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourOperatorTranslationJpaRepository
        extends JpaRepository<TourOperatorTranslationJpaEntity, TourOperatorTranslationId> {

    Optional<TourOperatorTranslationJpaEntity> findByTourOperatorIdAndLocale(UUID tourOperatorId, String locale);

    List<TourOperatorTranslationJpaEntity> findByTourOperatorId(UUID tourOperatorId);
}
