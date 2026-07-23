package com.vointika.shared.port;

import java.util.Optional;
import java.util.UUID;

/**
 * Cross-context lookup of an identity user by email. Used by the
 * {@code touroperator} team-invitation flow to enforce the already-a-member
 * guard (and, in the accept slice, to resolve an existing invitee).
 * Implemented in {@code identity.infrastructure.query.*}.
 *
 * <p>The email is matched against identity's normalized (lowercased) stored
 * form — callers pass an already-normalized value.
 */
public interface UserAccountQuery {

    Optional<UUID> findUserIdByEmail(String email);

    /**
     * The user's contact fields (email + name + language) for a transactional
     * email — e.g. the touroperator welcome email resolves the creator's
     * recipient info here before publishing, so the notification consumer never
     * queries identity. Empty if no account has this id.
     */
    Optional<UserContactView> findContact(UUID userId);

    /**
     * Batch account lookup — one query for a set of user ids, returning
     * {@link UserAccountView} (id + email + name) per resolvable account. An id
     * with no account is simply absent from the result (callers key by id and
     * treat a miss as null fields, never dropping their row). Empty input →
     * empty list. This is the roster's N+1-free enrichment path; the single-id
     * methods above stay for the invitation / order-lookup flows.
     */
    java.util.List<UserAccountView> findAccounts(java.util.Collection<UUID> userIds);
}
