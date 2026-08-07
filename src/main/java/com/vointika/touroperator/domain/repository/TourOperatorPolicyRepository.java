package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface TourOperatorPolicyRepository {

    /** Create-or-replace the whole {@code (operator, type)} row. */
    Policy upsert(Policy policy);

    Optional<Policy> findByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type);

    /**
     * The admin list, through the shared cursor framework (PATTERNS §4b).
     *
     * <p>The set is small — four rows at most — but it is <b>tenant</b> data, and
     * §4b's exemption is for curated <em>platform</em> lists (timezones,
     * currencies), not for a per-operator table. Going through the framework also
     * buys the filter and sort grammar every other tenant list speaks.
     */
    CursorPage<Policy> list(ListQuery query);

    /** Removes the row if present; returns whether one existed. */
    boolean deleteByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type);
}
