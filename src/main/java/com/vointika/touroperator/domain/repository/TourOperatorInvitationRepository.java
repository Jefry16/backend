package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.TourOperatorInvitation;

import java.util.Optional;
import java.util.UUID;

public interface TourOperatorInvitationRepository {
    TourOperatorInvitation save(TourOperatorInvitation invitation);

    /** Lookup by the at-rest token hash — the accept-link capability. */
    Optional<TourOperatorInvitation> findByTokenHash(String tokenHash);

    /**
     * Loads an invitation scoped to its operator — an id belonging to a
     * different operator resolves empty, so admin actions can't reach across
     * tenants via a guessed invitation id.
     */
    Optional<TourOperatorInvitation> findByIdAndTourOperatorId(UUID id, UUID tourOperatorId);

    /** Whether a live (PENDING) invitation already exists for this operator + email. */
    boolean existsPendingByTourOperatorIdAndEmail(UUID tourOperatorId, String email);
}
