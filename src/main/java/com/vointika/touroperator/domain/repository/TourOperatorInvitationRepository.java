package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.repository.TourOperatorInvitationRepository;
import com.vointika.shared.list.CursorPage;
import com.vointika.shared.list.ListQuery;
import com.vointika.touroperator.domain.entity.TourOperatorInvitation;

import com.vointika.shared.exception.ResourceNotFoundException;

import java.util.Optional;
import java.util.function.Supplier;
import java.util.UUID;

public interface TourOperatorInvitationRepository {

    /**
     * <b>Everything the invitee flow refuses answers this.</b> An unknown token, a
     * token for another operator, a missing operator behind a valid token, and a
     * missing user account behind one all say the same thing on purpose — an
     * anonymous caller holding a link must not learn which of those it is.
     * It was eight literals across five files.
     */
    Supplier<ResourceNotFoundException> NOT_FOUND =
            () -> new ResourceNotFoundException("Invitation not found");

    TourOperatorInvitation save(TourOperatorInvitation invitation);

    /** All invitations for an operator (any status), cursor-paginated + filtered. */
    CursorPage<TourOperatorInvitation> list(ListQuery query);

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
