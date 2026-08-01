package com.vointika.rendering.infrastructure.security;

import com.vointika.shared.web.security.PublicRoute;
import com.vointika.shared.web.security.PublicRouteRegistrar;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The storefront's server-to-server surface is exempt from JWT authentication —
 * but not from authentication. {@code InternalApiSecretFilter} gates every
 * {@code /api/storefront/**} path on the shared secret and short-circuits a
 * mismatch with 401 before any handler runs; this registrar only stops Spring
 * Security's {@code anyRequest().authenticated()} from demanding a JWT that a
 * server-to-server caller will never have.
 */
@Component
public class RenderingPublicRoutes implements PublicRouteRegistrar {

    @Override
    public List<PublicRoute> publicRoutes() {
        return List.of(
                new PublicRoute(HttpMethod.GET, "/api/storefront/render-context/*/shop"),
                new PublicRoute(HttpMethod.POST, "/api/storefront/*/verify-password")
        );
    }
}
