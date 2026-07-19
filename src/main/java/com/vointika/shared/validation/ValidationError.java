package com.vointika.shared.validation;

/**
 * One structured validation failure. {@code path} uses JSONPath-ish dot
 * notation rooted at {@code $} so clients can highlight the offending
 * field in a form (e.g. {@code $.sections.01HXYZ-hero.overrides.text} or
 * {@code $.order[2]}).
 *
 * <p>Use {@link com.vointika.shared.exception.ValidationFailedException}
 * to surface a list of these to the client; the global exception handler
 * maps that to a {@code 422 Unprocessable Content} response with the
 * errors carried in the body.
 */
public record ValidationError(String path, String message) {}
