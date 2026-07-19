package com.vointika.shared.web.response;

import com.vointika.shared.validation.ValidationError;

import java.time.Instant;
import java.util.List;

/**
 * Response body for {@code 422 Unprocessable Content} when the request
 * fails structured validation. Mirrors {@link ApiErrorResponse}'s shape
 * and adds an {@code errors} array — one entry per field-level failure
 * — so the client can highlight every offending field at once instead
 * of round-tripping through error messages.
 */
public record FieldValidationErrorResponse(
        int status,
        String error,
        String message,
        Instant timestamp,
        List<ValidationError> errors
) {}
