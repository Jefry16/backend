package com.vointika.shared.port;

import java.util.UUID;

/**
 * Identity-side provisioning for the team-invitation accept flow (epic
 * decision 1): the {@code touroperator} orchestrator resolves or creates the
 * invitee's account and issues a login-equivalent session, without importing
 * anything from {@code identity} (§3.3). Implemented in
 * {@code identity.infrastructure.port.*}.
 */
public interface InvitedUserProvisioning {

    /**
     * Finds the user by (already-normalized) email, else creates one that is
     * **VERIFIED from birth** (epic decision 3: possessing the emailed accept
     * link proves mailbox ownership, so the register/verification-email path
     * is deliberately skipped). Name and password are validated with
     * identity's own rules (422 on violation) — they are only consulted on
     * the create branch.
     *
     * <p>The {@code created} flag lets the caller distinguish the branches:
     * the accept flow pre-checks existence and treats a find here as a
     * concurrent-registration race → it refuses rather than attach
     * membership/session to an account whose password the caller never
     * proved (the pre-registered-email attack).
     */
    ProvisionedUser findOrCreateVerifiedUser(String email, String name, String rawPassword);

    /**
     * Login-equivalent session issuance for a just-provisioned invitee
     * (access JWT + a root refresh token, hash-at-rest) so they land in the
     * app without a second login step.
     */
    SessionTokens issueSession(UUID userId);

    record ProvisionedUser(UUID userId, boolean created) {}

    record SessionTokens(String accessToken, String refreshToken) {}
}
