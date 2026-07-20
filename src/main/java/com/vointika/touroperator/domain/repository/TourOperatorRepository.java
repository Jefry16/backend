package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.TourOperator;

import java.util.Optional;
import java.util.UUID;

public interface TourOperatorRepository {
    TourOperator save(TourOperator tourOperator);

    Optional<TourOperator> findById(UUID id);

    /** Whether any operator already holds this slug (global uniqueness). */
    boolean existsBySlug(String slug);

    /** Whether this owner already has an operator with this name (case-insensitive). */
    boolean existsByOwnerAndName(UUID createdBy, String name);
}
