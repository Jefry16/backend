package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.Policy;
import com.vointika.touroperator.domain.enums.PolicyType;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;

import java.util.Optional;
import java.util.UUID;

public interface TourOperatorPolicyRepository {

    Policy save(Policy policy);

    /**
     * <b>Always tenant-scoped.</b> A policy id is a UUID a caller can guess or
     * carry over from another operator, and the membership interceptor only
     * proves they belong to the operator in the <em>path</em> — so the lookup
     * itself has to bind the two, or one operator reads another's document. The
     * IDOR lesson, applied at the query rather than the router.
     */
    Optional<Policy> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** Whether this operator has already written that type — the create 409. */
    boolean existsByTourOperatorIdAndType(UUID tourOperatorId, PolicyType type);

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
    boolean deleteById(UUID id);
}
