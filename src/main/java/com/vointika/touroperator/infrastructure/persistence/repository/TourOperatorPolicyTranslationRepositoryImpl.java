package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.PolicyTranslation;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.touroperator.domain.repository.TourOperatorPolicyTranslationRepository;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorPolicyTranslationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorPolicyTranslationRepositoryImpl
        implements TourOperatorPolicyTranslationRepository {

    private final TourOperatorPolicyTranslationJpaRepository jpaRepository;

    public TourOperatorPolicyTranslationRepositoryImpl(
            TourOperatorPolicyTranslationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public PolicyTranslation upsert(PolicyTranslation translation) {
        return TourOperatorPolicyTranslationMapper.toDomain(
                jpaRepository.save(TourOperatorPolicyTranslationMapper.toJpa(translation)));
    }

    @Override
    public Optional<PolicyTranslation> find(UUID tourOperatorId, PolicyType type, String locale) {
        return jpaRepository.findByTourOperatorIdAndTypeAndLocale(tourOperatorId, type, locale)
                .map(TourOperatorPolicyTranslationMapper::toDomain);
    }

    @Override
    public List<PolicyTranslation> findAllByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type) {
        return jpaRepository.findByTourOperatorIdAndTypeOrderByLocaleAsc(tourOperatorId, type).stream()
                .map(TourOperatorPolicyTranslationMapper::toDomain)
                .toList();
    }

    @Override
    public boolean delete(UUID tourOperatorId, PolicyType type, String locale) {
        return jpaRepository.deleteByTourOperatorIdAndTypeAndLocale(tourOperatorId, type, locale) > 0;
    }
}
