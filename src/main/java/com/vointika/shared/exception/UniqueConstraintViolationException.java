package com.vointika.shared.exception;

/**
 * A write lost a race against a unique constraint: by the time it reached the
 * database, the row it meant to create already existed.
 *
 * <p>Thrown by the {@code TransactionRunner} adapter, which is the only place
 * that sees the persistence provider's own exception. A use case catches this
 * instead of a framework type, so the application layer stays free of Spring
 * (ArchUnit enforces that).
 *
 * <p>Deliberately narrower than the exception it replaces. The adapter translates
 * only a <em>duplicate-key</em> failure; a foreign-key, not-null or check-constraint
 * violation is a bug, not a race, and is left to surface as a 500. Catching the
 * broad parent — which 21 use cases used to do — meant a genuine defect could be
 * silently handled as "someone else got there first".
 *
 * <p>Uncaught, it maps to 409: the resource does exist, the caller just did not
 * win. Most callers catch it, because the honest response is usually specific to
 * the operation (a named conflict, or — in registration — a deliberately generic
 * answer that refuses to confirm the address is taken).
 */
public class UniqueConstraintViolationException extends DomainException {

    public UniqueConstraintViolationException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
