package com.vointika.shared.web.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * SPI for a context that owns whole <b>hosts</b> rather than individual routes, to
 * say that an unauthenticated miss there is a <b>404, not a 401</b>.
 *
 * <p>Mirrors {@link PublicRouteRegistrar} and {@link RateLimitRuleRegistrar}: the
 * rule lives with the context that owns the surface, never in a central
 * hardcoded map. It exists as an SPI because it has to — {@code shared} is the
 * kernel and may not import a context, so the entry point cannot ask the
 * storefront a question except through an interface the kernel declares.
 *
 * <p><b>The problem it solves.</b> {@code PublicRoute} patterns are an allowlist,
 * so <em>every</em> way of missing one fails closed — and on a public page failing
 * closed is the wrong answer. A path variable is constrained
 * ({@code /policies/{type:[a-z]+(?:-[a-z]+)*}}), so a visitor who types
 * {@code /policies/legal_notice} matches no route, is never seen by MVC, and is
 * told {@code 401 Authentication required} by a site that has no authentication
 * at all. The same reached {@code /pages/About_Us} and
 * {@code /experiences/Sunset_Sail}.
 *
 * <p><b>It does not widen the allowlist by one byte.</b> The
 * {@code authorizeHttpRequests} chain is untouched; only the response to a
 * request that already lost is reshaped. Nothing becomes reachable that was not
 * reachable before — the constrained patterns still decide what MVC ever sees,
 * which is the property {@code CLAUDE.md} protects when it says an unconstrained
 * path variable is a hole.
 */
public interface UnauthenticatedRequestPolicy {

    /**
     * The 404 message to answer this request with instead of 401, or empty to
     * leave it a 401.
     *
     * <p>Returning a message is a claim that the caller could never have
     * authenticated here anyway — the surface is public, so "you are not
     * authenticated" is not a true statement about why the request failed.
     */
    Optional<String> notFoundMessage(HttpServletRequest request);
}
