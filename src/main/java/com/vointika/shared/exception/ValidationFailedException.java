package com.vointika.shared.exception;

import com.vointika.shared.validation.ValidationError;

import java.util.List;

/**
 * Thrown when an input fails structured validation — used in particular
 * by the storefront section-config validator. Carries the full list of
 * field-level errors so the client receives a path-keyed report rather
 * than a single concatenated string. The global exception handler maps
 * this to a {@code 422 Unprocessable Content} response.
 *
 * <p>Distinct from {@link InvalidFieldException}, which represents a
 * single invariant breach with one message.
 */
public class ValidationFailedException extends RuntimeException {

    private final List<ValidationError> errors;

    public ValidationFailedException(List<ValidationError> errors) {
        super("Validation failed (" + (errors == null ? 0 : errors.size()) + " error"
                + (errors != null && errors.size() == 1 ? "" : "s") + ")");
        if (errors == null || errors.isEmpty()) {
            throw new IllegalArgumentException(
                    "ValidationFailedException requires at least one error");
        }
        this.errors = List.copyOf(errors);
    }

    public List<ValidationError> getErrors() {
        return errors;
    }
}
