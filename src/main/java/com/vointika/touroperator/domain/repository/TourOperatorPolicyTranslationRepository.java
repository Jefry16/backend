package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.PolicyTranslation;
import com.vointika.touroperator.domain.enums.PolicyType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TourOperatorPolicyTranslationRepository {

    /** Create-or-replace the whole {@code (operator, type, locale)} overlay row. */
    PolicyTranslation upsert(PolicyTranslation translation);

    Optional<PolicyTranslation> find(UUID tourOperatorId, PolicyType type, String locale);

    /** Every translated locale for one policy (one row each). */
    List<PolicyTranslation> findAllByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type);

    /** Removes the row if present; returns whether one existed. */
    boolean delete(UUID tourOperatorId, PolicyType type, String locale);
}
