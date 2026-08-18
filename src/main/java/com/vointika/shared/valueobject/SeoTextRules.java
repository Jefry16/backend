package com.vointika.shared.valueobject;

import com.vointika.shared.exception.InvalidFieldException;

/**
 * The checks {@link SeoTitle} and {@link SeoDescription} share. Package-private:
 * the two records are the API, this is only where their common body lives.
 *
 * <p><b>The control-character scan runs before the trim, deliberately.</b> Trimming
 * first would silently accept a leading newline instead of refusing it, because
 * {@code String.trim} strips exactly the low control characters this rejects. All
 * six of the records that preceded this ordered it this way; keeping the order is
 * what makes the collapse a refactor rather than a loosening.
 */
final class SeoTextRules {

    private SeoTextRules() {
    }

    static String normalize(String raw, String fieldName, int maxLength) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException(fieldName + " cannot be blank");
        }
        for (int i = 0, len = raw.length(); i < len; i++) {
            int type = Character.getType(raw.charAt(i));
            if (type == Character.CONTROL || type == Character.FORMAT) {
                throw new InvalidFieldException(fieldName + " contains an invalid character");
            }
        }
        // Non-blank above, so the trim cannot empty it — only the upper bound is left.
        String trimmed = raw.trim();
        if (trimmed.length() > maxLength) {
            throw new InvalidFieldException(
                    fieldName + " must be between 1 and " + maxLength + " characters");
        }
        return trimmed;
    }
}
