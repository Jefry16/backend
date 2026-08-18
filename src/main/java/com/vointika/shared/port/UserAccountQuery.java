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

    /**
     * The refusal when an authenticated principal resolves to no account — a token
     * that verified but whose user is gone.
     *
     * <p>It is a 401 rather than a 404 because the failure is the *caller's identity*,
     * not a missing resource: the right client response is to re-authenticate. Six
     * sites said it, in two contexts — identity's five self-service use cases and
     * {@code touroperator}'s create, which reaches a user only through this seam.
     *
     * <p>Sameness is not load-bearing here the way the tenant 404's is; nothing can be
     * enumerated with it. It is one sentence about one condition, so it is written once.
     */
    String INVALID_PRINCIPAL = "Invalid authenticated user";

    Optional<UUID> findUserIdByEmail(String email);

    /**
     * The user's contact fields (email + name + language) for a transactional
     * email — e.g. the touroperator welcome email resolves the creator's
     * recipient info here before publishing, so the notification consumer never
     * queries identity. Empty if no account has this id.
     */
    Optional<UserContactView> findContact(UUID userId);
}
