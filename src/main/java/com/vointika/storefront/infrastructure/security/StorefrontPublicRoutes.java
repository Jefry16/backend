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
 */
@Component
public class StorefrontPublicRoutes implements PublicRouteRegistrar {

    @Override
    public List<PublicRoute> publicRoutes() {
        return List.of(
                new PublicRoute(HttpMethod.GET, "/")
        );
    }
}
