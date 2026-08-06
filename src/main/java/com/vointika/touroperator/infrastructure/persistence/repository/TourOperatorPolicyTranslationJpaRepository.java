package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyTranslationId;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourOperatorPolicyTranslationJpaRepository
        extends JpaRepository<TourOperatorPolicyTranslationJpaEntity, TourOperatorPolicyTranslationId> {

    /** Every policy's overlay for one locale, so the footer costs one query rather than four. */
    List<TourOperatorPolicyTranslationJpaEntity> findByTourOperatorIdAndLocale(UUID tourOperatorId, String locale);

    Optional<TourOperatorPolicyTranslationJpaEntity> findByTourOperatorIdAndTypeAndLocale(
            UUID tourOperatorId, PolicyType type, String locale);
}
