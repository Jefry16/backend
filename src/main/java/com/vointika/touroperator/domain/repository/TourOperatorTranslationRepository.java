package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.TourOperatorTranslation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourOperatorTranslationRepository {

    /** Create-or-replace the whole (operator, locale) overlay row. */
    TourOperatorTranslation upsert(TourOperatorTranslation translation);

    Optional<TourOperatorTranslation> findByTourOperatorIdAndLocale(UUID tourOperatorId, String locale);

    /** All translated locales for an operator (one row each). */
    List<TourOperatorTranslation> findAllByTourOperatorId(UUID tourOperatorId);

    /** Removes the (operator, locale) row if present; returns whether one existed. */
    boolean deleteByTourOperatorIdAndLocale(UUID tourOperatorId, String locale);
}
