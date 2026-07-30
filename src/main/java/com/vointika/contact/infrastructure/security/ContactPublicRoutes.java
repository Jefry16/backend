package com.vointika.contact.infrastructure.security;

import com.vointika.shared.web.security.PublicRoute;
import com.vointika.shared.web.security.PublicRouteRegistrar;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The intake endpoint is server-to-server like every {@code /api/internal/**}
 * route: exempt from JWT, still gated by {@code InternalApiSecretFilter}.
 *
 * <p>It lives in {@code contact} rather than with the render-context routes
 * because it MUTATES, and PATTERNS §8c puts an internal write with the context
 * that owns the data.
 */
@Component
public class ContactPublicRoutes implements PublicRouteRegistrar {

    @Override
    public List<PublicRoute> publicRoutes() {
        return List.of(
                new PublicRoute(HttpMethod.POST, "/api/internal/storefront/*/contact-messages")
        );
    }
}
