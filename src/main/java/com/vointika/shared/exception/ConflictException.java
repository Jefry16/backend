package com.vointika.shared.exception;

/**
 * Maps to HTTP 409. Use when the request can't be fulfilled because the
 * target resource is in a state that conflicts with the operation —
 * but the conflict is NOT a duplicate (use {@link ResourceAlreadyExistsException}
 * for that). Examples: "slot is no longer OPEN", "slot is fully booked
 * for this audience", "cart is already checked out".
 *
 * <p>If the conflict is a unique-constraint duplicate (slug collision,
 * email already registered, etc.), throw {@link ResourceAlreadyExistsException}
 * instead.
 */
public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, String errorCode) {
        super(message, errorCode);
    }
}
