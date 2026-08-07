package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyTranslationId;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorPolicyTranslationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourOperatorPolicyTranslationJpaRepository
        extends JpaRepository<TourOperatorPolicyTranslationJpaEntity, TourOperatorPolicyTranslationId> {

    /** Every policy's overlay for one locale, so the footer costs one query rather than four. */
    List<TourOperatorPolicyTranslationJpaEntity> findByTourOperatorIdAndLocale(UUID tourOperatorId, String locale);

    Optional<TourOperatorPolicyTranslationJpaEntity> findByTourOperatorIdAndTypeAndLocale(
            UUID tourOperatorId, PolicyType type, String locale);

    /** One policy's overlays, ordered so the admin list is stable between requests. */
    List<TourOperatorPolicyTranslationJpaEntity> findByTourOperatorIdAndTypeOrderByLocaleAsc(
            UUID tourOperatorId, PolicyType type);

    @Modifying
    long deleteByTourOperatorIdAndTypeAndLocale(UUID tourOperatorId, PolicyType type, String locale);
}
