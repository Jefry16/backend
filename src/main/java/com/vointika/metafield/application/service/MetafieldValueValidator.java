package com.vointika.metafield.application.service;

import com.vointika.metafield.domain.valueobject.MetafieldType;
import com.vointika.shared.exception.InvalidFieldException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Validates a submitted metafield value against its definition's type and
 * returns the canonical string form to store. Numbers, booleans and dates are
 * normalized ({@code "007"} → {@code "7"}, {@code "TRUE"} → {@code "true"})
 * so a later typed exposure never re-parses surprises; text, URL and JSON
 * keep the trimmed original.
 *
 * <p>A blank value is always rejected — clearing a value is the DELETE
 * endpoint's job, not an empty PUT.
 */
public class MetafieldValueValidator {

    private static final int SINGLE_LINE_MAX = 255;
    private static final int MULTI_LINE_MAX = 5_000;
    private static final int DECIMAL_MAX_CHARS = 40;
    private static final int DECIMAL_MAX_DIGITS = 38;
    private static final int URL_MAX = 2_000;
    private static final int JSON_MAX = 10_000;

    private final ObjectMapper objectMapper;

    public MetafieldValueValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * @return the canonical form to persist
     * @throws InvalidFieldException when the value doesn't fit the type (422)
     */
    public String validateAndNormalize(MetafieldType type, String raw) {
        if (raw == null || raw.isBlank()) {
            throw new InvalidFieldException(
                    "Metafield value cannot be blank — use DELETE to clear a value");
        }
        String trimmed = raw.strip();
        return switch (type) {
            case SINGLE_LINE_TEXT -> singleLineText(trimmed);
            case MULTI_LINE_TEXT -> multiLineText(trimmed);
            case NUMBER_INTEGER -> numberInteger(trimmed);
            case NUMBER_DECIMAL -> numberDecimal(trimmed);
            case BOOLEAN -> bool(trimmed);
            case DATE -> date(trimmed);
            case URL -> url(trimmed);
            case JSON -> json(trimmed);
            case METAOBJECT_REFERENCE -> metaobjectReference(trimmed);
        };
    }

    /**
     * Shape-only here (a canonical UUID string); the upsert use case checks
     * the entry exists, belongs to the operator and matches the definition's
     * pinned metaobject type — that needs repositories this service doesn't.
     */
    private String metaobjectReference(String value) {
        try {
            return java.util.UUID.fromString(value).toString();
        } catch (IllegalArgumentException e) {
            throw new InvalidFieldException(
                    "A metaobject_reference metafield value must be a metaobject id");
        }
    }

    private String singleLineText(String value) {
        if (value.length() > SINGLE_LINE_MAX) {
            throw new InvalidFieldException(
                    "A single_line_text metafield value must be at most " + SINGLE_LINE_MAX + " characters");
        }
        rejectControlChars(value, false, "single_line_text");
        return value;
    }

    private String multiLineText(String value) {
        if (value.length() > MULTI_LINE_MAX) {
            throw new InvalidFieldException(
                    "A multi_line_text metafield value must be at most " + MULTI_LINE_MAX + " characters");
        }
        rejectControlChars(value, true, "multi_line_text");
        return value;
    }

    private String numberInteger(String value) {
        try {
            return Long.toString(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new InvalidFieldException("A number_integer metafield value must be a whole number");
        }
    }

    private String numberDecimal(String value) {
        if (value.length() > DECIMAL_MAX_CHARS) {
            throw new InvalidFieldException(
                    "A number_decimal metafield value must be at most " + DECIMAL_MAX_CHARS + " characters");
        }
        BigDecimal parsed;
        try {
            parsed = new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new InvalidFieldException("A number_decimal metafield value must be a decimal number");
        }
        // The char cap doesn't bound the exponent: "1E2147483647" is 12 chars
        // but toPlainString() would materialize ~2.1 billion zeros (OOM on the
        // shared JVM). Bound precision AND scale before expanding.
        if (parsed.precision() > DECIMAL_MAX_DIGITS || Math.abs(parsed.scale()) > DECIMAL_MAX_DIGITS) {
            throw new InvalidFieldException("A number_decimal metafield value is out of range");
        }
        return parsed.toPlainString();
    }

    private String bool(String value) {
        if (value.equalsIgnoreCase("true")) {
            return "true";
        }
        if (value.equalsIgnoreCase("false")) {
            return "false";
        }
        throw new InvalidFieldException("A boolean metafield value must be true or false");
    }

    private String date(String value) {
        try {
            return LocalDate.parse(value).toString();
        } catch (DateTimeParseException e) {
            throw new InvalidFieldException(
                    "A date metafield value must be an ISO-8601 calendar date (e.g. 2026-08-01)");
        }
    }

    private String url(String value) {
        if (value.length() > URL_MAX) {
            throw new InvalidFieldException(
                    "A url metafield value must be at most " + URL_MAX + " characters");
        }
        URI uri;
        try {
            uri = new URI(value);
        } catch (URISyntaxException e) {
            throw new InvalidFieldException("A url metafield value must be a valid URL");
        }
        String scheme = uri.getScheme();
        boolean httpish = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        if (!httpish || uri.getHost() == null || uri.getHost().isEmpty()) {
            throw new InvalidFieldException("A url metafield value must be an absolute http(s) URL");
        }
        return value;
    }

    private String json(String value) {
        if (value.length() > JSON_MAX) {
            throw new InvalidFieldException(
                    "A json metafield value must be at most " + JSON_MAX + " characters");
        }
        try {
            // readValue (not readTree) so trailing-token garbage is rejected
            // instead of silently stored.
            objectMapper.readValue(value, JsonNode.class);
        } catch (Exception e) {
            throw new InvalidFieldException("A json metafield value must be valid JSON");
        }
        return value;
    }

    private static void rejectControlChars(String value, boolean allowNewlines, String typeCode) {
        for (int i = 0, len = value.length(); i < len; i++) {
            char c = value.charAt(i);
            if (allowNewlines && (c == '\n' || c == '\r' || c == '\t')) {
                continue;
            }
            int charType = Character.getType(c);
            if (charType == Character.CONTROL || charType == Character.FORMAT) {
                throw new InvalidFieldException(
                        "A " + typeCode + " metafield value contains an invalid character");
            }
        }
    }
}
