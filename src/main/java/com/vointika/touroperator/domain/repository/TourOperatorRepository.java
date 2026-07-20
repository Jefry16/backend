package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.TourOperator;

public interface TourOperatorRepository {
    TourOperator save(TourOperator tourOperator);

    /** Whether any operator already holds this slug (global uniqueness). */
    boolean existsBySlug(String slug);

    /** Whether this owner already has an operator with this name (case-insensitive). */
    boolean existsByOwnerAndName(java.util.UUID createdBy, String name);
}
