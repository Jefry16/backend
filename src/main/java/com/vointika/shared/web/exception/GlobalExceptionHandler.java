package com.vointika.shared.web.exception;

import com.vointika.shared.web.response.ApiErrorResponse;
import com.vointika.shared.exception.*;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- Domain exceptions ---

    @ExceptionHandler(InvalidFieldException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidField(InvalidFieldException ex) {
        return build(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), ex.getErrorCode());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ApiErrorResponse> handleAlreadyExists(ResourceAlreadyExistsException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), ex.getErrorCode());
    }

    @ExceptionHandler(GoneException.class)
    public ResponseEntity<ApiErrorResponse> handleGone(GoneException ex) {
        return build(HttpStatus.GONE, ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex) {
        return build(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    // --- Client input errors that Spring MVC surfaces as typed exceptions ---

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String param = ex.getName();
        return build(HttpStatus.BAD_REQUEST, "Invalid value for parameter '" + param + "'");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return build(HttpStatus.BAD_REQUEST, ex.getMessage() != null ? ex.getMessage() : "Invalid argument");
    }

    // --- True unexpected errors (last resort) ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    // --- Override ResponseEntityExceptionHandler so all Spring MVC framework
    // exceptions (malformed JSON, wrong Content-Type, wrong method, etc.) return
    // the ApiErrorResponse shape with proper 4xx status codes. ---

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             @NonNull HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             @NonNull WebRequest request) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        String reason = status != null ? status.getReasonPhrase() : "Error";
        String message = safeMessage(ex, reason);

        if (status != null && status.is5xxServerError()) {
            log.error("Spring MVC exception mapped to {}", status, ex);
        }

        Object apiError = new ApiErrorResponse(
                statusCode.value(),
                reason,
                message,
                null,
                Instant.now()
        );
        return new ResponseEntity<>(apiError, headers, statusCode);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message) {
        return build(status, message, null);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String message, String code) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                message,
                code,
                Instant.now()
        ));
    }

    private static String safeMessage(Exception ex, String fallback) {
        if (ex instanceof org.springframework.http.converter.HttpMessageNotReadableException) {
            Throwable cause = ex.getCause();
            if (cause == null) {
                return "Request body is required";
            }
            if (cause instanceof com.fasterxml.jackson.databind.exc.MismatchedInputException) {
                return "Request body must be a JSON object";
            }
            return "Malformed request body";
        }
        return fallback;
    }
}
