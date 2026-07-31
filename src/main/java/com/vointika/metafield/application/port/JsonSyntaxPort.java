package com.vointika.metafield.application.port;

/**
 * Is this string one complete, well-formed JSON document?
 *
 * <p>The whole reason a JSON parser was ever reachable from the application layer.
 * Stated as a question the domain can ask, so the answer's machinery — and the
 * library that provides it — stays in infrastructure.
 *
 * <p>"Complete" is the load-bearing word: {@code {"a":1} garbage} must be rejected,
 * not silently accepted with the tail ignored.
 */
public interface JsonSyntaxPort {
    boolean isWellFormed(String value);
}
