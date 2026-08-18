package com.vointika.touroperator.domain.repository;

import com.vointika.shared.exception.ResourceNotFoundException;
import com.vointika.shared.port.TourOperatorMembershipCheck;
import com.vointika.touroperator.domain.entity.TourOperator;

import java.util.Optional;
import java.util.UUID;

public interface TourOperatorRepository {
    TourOperator save(TourOperator tourOperator);

    Optional<TourOperator> findById(UUID id);

    /**
     * The same lookup, or the tenant-isolation 404 — the shape a dozen callers
     * wanted. The message is {@link TourOperatorMembershipCheck#TENANT_NOT_FOUND}
     * because a missing operator and a non-member must answer identically.
     */
    default TourOperator requireById(UUID id) {
        return findById(id).orElseThrow(
                () -> new ResourceNotFoundException(TourOperatorMembershipCheck.TENANT_NOT_FOUND));
    }

    /** Whether any operator already holds this handle (global uniqueness). */
    boolean existsByHandle(String handle);

    /** Whether this owner already has an operator with this name (case-insensitive). */
    boolean existsByOwnerAndName(UUID createdBy, String name);
}
