package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.TourOperatorTranslation;
import com.vointika.touroperator.domain.repository.TourOperatorTranslationRepository;
import com.vointika.touroperator.infrastructure.persistence.entity.TourOperatorTranslationId;
import com.vointika.touroperator.infrastructure.persistence.mapper.TourOperatorTranslationMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class TourOperatorTranslationRepositoryImpl implements TourOperatorTranslationRepository {

    private final TourOperatorTranslationJpaRepository jpaRepository;

    public TourOperatorTranslationRepositoryImpl(TourOperatorTranslationJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public TourOperatorTranslation upsert(TourOperatorTranslation translation) {
        // save() merges on the assigned composite id → create-or-replace the row.
        return TourOperatorTranslationMapper.toDomain(
                jpaRepository.save(TourOperatorTranslationMapper.toJpa(translation)));
    }

    @Override
    public Optional<TourOperatorTranslation> findByTourOperatorIdAndLocale(UUID tourOperatorId, String locale) {
        return jpaRepository.findByTourOperatorIdAndLocale(tourOperatorId, locale)
                .map(TourOperatorTranslationMapper::toDomain);
    }

    @Override
    public List<TourOperatorTranslation> findAllByTourOperatorId(UUID tourOperatorId) {
        return jpaRepository.findByTourOperatorId(tourOperatorId).stream()
                .map(TourOperatorTranslationMapper::toDomain)
                .toList();
    }

    @Override
    public boolean deleteByTourOperatorIdAndLocale(UUID tourOperatorId, String locale) {
        TourOperatorTranslationId id = new TourOperatorTranslationId(tourOperatorId, locale);
        if (!jpaRepository.existsById(id)) {
            return false;
        }
        jpaRepository.deleteById(id);
        return true;
    }
}
