package com.vointika.shared.exception;

public abstract class DomainException extends RuntimeException {

    /**
     * Optional machine-readable code surfaced on {@code ApiErrorResponse.code}
     * so callers can branch on a stable token instead of the human (and
     * localizable) {@code message}. Null when the throw site has nothing more
     * specific to say than its HTTP status — the common case.
     */
    private final String errorCode;

    public DomainException(String message) {
        this(message, null);
    }

    public DomainException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
