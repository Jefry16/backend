package com.vointika.shared.port;

import java.time.Duration;

/**
 * Fixed-window rate limiting over a shared counter store (Redis in
 * production — single-key INCR+EXPIRE, so it works across app replicas).
 *
 * <p><b>Fail-open contract:</b> implementations return {@code true} (allow)
 * when the counter store is unreachable — rate limiting degrades, requests
 * never fail because Redis is down. Consistent with the inventory-hold
 * adapter's fail-open philosophy (§2).
 *
 * <p>Consumed by the three rate-limiting layers (see {@code PATTERNS.md}): the
 * per-IP {@code EndpointRateLimitFilter}, the coarse per-user
 * {@code ApiRateLimitFilter}, and per-account throttles in the use cases. Keys are
 * plain namespaced strings ({@code rl:{dimension}:…}); callers own their namespace.
 */
public interface RateLimiterPort {

    /**
     * Counts one attempt against {@code key} and reports whether it is within
     * {@code limit} attempts per {@code window}. The first attempt in a
     * window starts it; the window does not slide.
     *
     * @return {@code true} if the attempt is allowed (within the limit, or
     *         the store is unreachable — fail-open), {@code false} if the
     *         caller should reject the request.
     */
    boolean tryAcquire(String key, int limit, Duration window);
}
