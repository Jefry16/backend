package com.vointika.shared.service;

import com.vointika.shared.exception.InvalidFieldException;

import java.util.UUID;

/**
 * Static UUID validation helpers used at boundary layers.
 *
 * <p>Lifted from the {@code cart} context's controller when a second
 * caller appeared ({@code rendering}'s cart-page render-context endpoint),
 * per §12.2 — abstract only when the second use case actually shows up,
 * not speculatively.
 */
public final class Uuids {

    private Uuids() {}

    /**
     * Asserts that an SSR-supplied id is UUID v7 — the project's id
     * convention (time-sortable, B-tree-friendly). Used at the controller
     * boundary on the cart-id-as-session-token routes, where the cart-id
     * is the codebase's one client-generated id (per the BFF model).
     * Throws {@link InvalidFieldException} (→ 422) on any other version.
     */
    public static void requireUuidV7(UUID id, String fieldName) {
        if (id.version() != 7) {
            throw new InvalidFieldException(fieldName + " must be UUID v7");
        }
    }

    /**
     * Parses an SSR-supplied UUID v7 from its string form, raising
     * {@link InvalidFieldException} (→ 422) for non-UUID strings and
     * non-v7 UUIDs alike. Returns {@code null} when {@code raw} is null
     * or blank — callers that need a "missing means error" contract
     * should check the return separately. Lets the controller boundary
     * own the full set of malformed-id responses rather than letting
     * Spring's {@code MethodArgumentTypeMismatchException} surface as a
     * 400 for unparseable strings.
     */
    public static UUID parseOptionalUuidV7(String raw, String fieldName) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        UUID parsed;
        try {
            parsed = UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new InvalidFieldException(fieldName + " must be UUID v7");
        }
        requireUuidV7(parsed, fieldName);
        return parsed;
    }
}
