package com.vointika.identity.domain.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

public record Password(String value) {

    public Password {
        if (value == null || value.isBlank()) {
            throw new InvalidFieldException("Password cannot be blank");
        }
        if (value.length() < 8) {
            throw new InvalidFieldException("Password must be at least 8 characters");
        }
        if (value.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            throw new InvalidFieldException("Password must be at most 72 bytes");
        }
        if (!value.matches(".*[A-Z].*")) {
            throw new InvalidFieldException("Password must contain at least one uppercase letter");
        }
        if (!value.matches(".*[a-z].*")) {
            throw new InvalidFieldException("Password must contain at least one lowercase letter");
        }
        if (!value.matches(".*[0-9].*")) {
            throw new InvalidFieldException("Password must contain at least one number");
        }
        if (!value.matches(".*[^A-Za-z0-9].*")) {
            throw new InvalidFieldException("Password must contain at least one special character");
        }
    }

    /**
     * Redacted so the plaintext never reaches logs, exception messages, or
     * debuggers via the record's default {@code toString()}. Use {@link #value()}
     * when the raw password is genuinely needed (e.g. hashing).
     */
    @Override
    public String toString() {
        return "Password[REDACTED]";
    }
}