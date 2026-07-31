package com.vointika.shared.port;

/**
 * How a use case reports something an operator or an on-call engineer needs to
 * see, without importing a logging library.
 *
 * <p>Reserved for facts the application layer genuinely owns and that are not
 * domain errors: a fire-and-forget side effect that failed, a security signal
 * worth alerting on, a branch taken because configuration was missing. If a
 * failure belongs to an adapter — a storage delete, a broker publish — it is
 * logged there instead, and never reaches a use case.
 *
 * <p>{@code source} keeps the logger named after the class that reported, so log
 * filtering still works; the adapter does not flatten everything to its own name.
 * Message placeholders are {@code {}}, and a trailing {@link Throwable} is
 * attached rather than formatted.
 */
public interface DiagnosticLogPort {

    void warn(Class<?> source, String message, Object... args);

    void info(Class<?> source, String message, Object... args);
}
