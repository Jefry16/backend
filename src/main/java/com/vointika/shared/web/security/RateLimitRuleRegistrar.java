package com.vointika.shared.web.security;

import java.util.List;

/**
 * SPI for a bounded context to declare its per-IP rate limits (layer A of the
 * rate-limiting model — see {@code PATTERNS.md}). The {@code EndpointRateLimitFilter}
 * collects every registrar and enforces the union. Mirrors
 * {@link PublicRouteRegistrar}: rules live with the context that owns the
 * endpoint, never in a central hardcoded map.
 */
public interface RateLimitRuleRegistrar {

    List<RateLimitRule> rateLimitRules();
}
