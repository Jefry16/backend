package com.vointika.shared.web.security;

import org.springframework.http.HttpMethod;

import java.time.Duration;

/**
 * A per-IP rate limit on one endpoint: at most {@code limit} requests per
 * {@code window} from a single client IP to {@code method} + {@code pathPattern}.
 *
 * <p>{@code pathPattern} is a Spring {@code PathPattern} (e.g.
 * {@code /api/invitations/*&#47;accept}); the {@code *} matches a single path
 * segment, so a path-variable route is limited correctly and rotating the
 * variable does not dodge the bucket (the counter keys on the pattern, not the
 * concrete URI). Declared per-context via {@link RateLimitRuleRegistrar}.
 */
public record RateLimitRule(HttpMethod method, String pathPattern, int limit, Duration window) {}
