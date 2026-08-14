package com.vointika.shared.port;

/**
 * A cross-context projection of an identity user's contact fields for
 * transactional email: the normalized (lowercased) {@code email}, display
 * {@code name}, and {@code language} (lowercase UI-locale code). Primitives
 * only — the identity value objects never cross into {@code shared} (PATTERNS §6).
 */
public record UserContactView(String email, String name, String language) {
}
