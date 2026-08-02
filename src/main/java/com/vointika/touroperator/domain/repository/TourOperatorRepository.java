package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.TourOperator;

import java.util.Optional;
import java.util.UUID;

public interface TourOperatorRepository {
    TourOperator save(TourOperator tourOperator);

    Optional<TourOperator> findById(UUID id);

    /** The operator holding this handle — the storefront's tenant lookup. */
    Optional<TourOperator> findByHandle(String handle);

    /** Whether any operator already holds this handle (global uniqueness). */
    boolean existsByHandle(String handle);

    /** Whether this owner already has an operator with this name (case-insensitive). */
    boolean existsByOwnerAndName(UUID createdBy, String name);
}
