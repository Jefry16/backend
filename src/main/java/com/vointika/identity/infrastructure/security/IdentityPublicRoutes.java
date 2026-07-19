package com.vointika.identity.infrastructure.security;

import com.vointika.shared.web.security.PublicRoute;
import com.vointika.shared.web.security.PublicRouteRegistrar;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class IdentityPublicRoutes implements PublicRouteRegistrar {

    @Override
    public List<PublicRoute> publicRoutes() {
        return List.of(
                new PublicRoute(HttpMethod.POST, "/api/auth/register"),
                new PublicRoute(HttpMethod.GET,  "/api/auth/verify"),
                new PublicRoute(HttpMethod.POST, "/api/auth/resend-verification"),
                new PublicRoute(HttpMethod.POST, "/api/auth/login"),
                new PublicRoute(HttpMethod.POST, "/api/auth/refresh"),
                new PublicRoute(HttpMethod.POST, "/api/auth/request-password-reset"),
                new PublicRoute(HttpMethod.POST, "/api/auth/reset-password"),
                // The supported UI-language list drives the frontend picker,
                // including on pre-login pages — non-sensitive.
                new PublicRoute(HttpMethod.GET, "/api/ui-languages")
        );
    }
}
