package com.vointika.shared.exception;

/**
 * A throttle inside a use case refused the request (PATTERNS §8a layer B).
 *
 * <p>The filter-based layers write their own 429 because they run before any
 * handler; a use case cannot, so it throws and the handler maps it. This is the
 * first layer-B throttle that has to REFUSE — the existing ones degrade instead
 * (registration still succeeds, it just stops emailing).
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}
