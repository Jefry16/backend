package com.vointika.storefront.infrastructure.security;

import com.vointika.shared.web.security.PublicRoute;
import com.vointika.shared.web.security.PublicRouteRegistrar;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The storefront is unauthenticated, and that is all it needs for now — not a
 * host-matched filter chain of its own. Nothing today wants storefront requests
 * to have <em>different</em> security, only <em>no</em> authentication:
 * {@code JwtAuthenticationFilter} leaves anonymous requests alone and
 * {@code ApiRateLimitFilter} passes through anything without a JWT principal.
 * Revisit when the storefront needs its own CORS, error shape or rate limit.
 *
 * <p><b>HEAD is registered separately, and has to be.</b> Spring MVC serves HEAD
 * from a {@code @GetMapping} for free, but Spring Security matches on the exact
 * method — so a GET-only entry rejects HEAD at the filter chain before MVC sees
 * it: a <b>401</b>, not a 405, in the JSON shape
 * {@code RestAuthenticationEntryPoint} writes. Wrong for a public page. RFC 9110
 * has HEAD identical to GET minus the body, and HEAD is what crawlers, link
 * checkers, uptime monitors and CDNs send. Found by running it against the built
 * stack, after the slice's tests and curls all used GET.
 *
 * <p>OPTIONS stays closed, deliberately: nothing preflights a server-rendered
 * same-origin page, so opening it would be a decision rather than a correction.
 */
@Component
public class StorefrontPublicRoutes implements PublicRouteRegistrar {

    @Override
    public List<PublicRoute> publicRoutes() {
        return List.of(
                new PublicRoute(HttpMethod.GET, "/"),
                new PublicRoute(HttpMethod.HEAD, "/")
        );
    }
}
