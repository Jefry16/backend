package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.enums.PolicyType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourOperatorPolicyRepository {

    /** Create-or-replace the whole {@code (operator, type)} row. */
    Policy upsert(Policy policy);

    Optional<Policy> findByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type);

    /**
     * Every policy the operator has written, ordered by type so the admin list is
     * stable between requests. Bounded at four by the enum, which is why this
     * returns a plain list rather than a {@code CursorPage} (PATTERNS §4b's
     * exemption for curated, bounded sets).
     */
    List<Policy> findAllByTourOperatorId(UUID tourOperatorId);

    /** Removes the row if present; returns whether one existed. */
    boolean deleteByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type);
}
