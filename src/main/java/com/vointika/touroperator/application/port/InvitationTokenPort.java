package com.vointika.touroperator.application.port;

/**
 * Generates and hashes team-invitation tokens — identity's verification-token
 * posture (an opaque random token in the emailed link, only its SHA-256 hash at
 * rest), mirrored rather than imported because identity's token ports are
 * context-internal.
 */
public interface InvitationTokenPort {

    /** A URL-safe opaque token for the accept link — never persisted raw. */
    String generate();

    /** The at-rest form of a token (deterministic, for lookup-by-token). */
    String hash(String rawToken);
}
