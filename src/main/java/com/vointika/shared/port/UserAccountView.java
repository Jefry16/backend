package com.vointika.shared.port;

import java.util.UUID;

/**
 * A cross-context identity account projection: the id plus display fields other
 * contexts need (e.g. the team roster). Primitives only — the identity
 * {@code Email}/{@code UserName} value objects never cross into {@code shared}
 * (§4.2). {@code email} is the normalized (lowercased) stored form.
 */
public record UserAccountView(UUID userId, String email, String name) {
}
