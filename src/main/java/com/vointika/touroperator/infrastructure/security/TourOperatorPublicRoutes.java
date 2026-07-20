package com.vointika.touroperator.infrastructure.security;

import com.vointika.shared.web.security.PublicRoute;
import com.vointika.shared.web.security.PublicRouteRegistrar;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The invitee-side accept routes are public — the emailed token is the
 * capability, and a brand-new invitee has no JWT. (The JWT filter still runs, so
 * a logged-in accepter arrives with a principal; the accept use case forks on it.)
 */
@Component
public class TourOperatorPublicRoutes implements PublicRouteRegistrar {

    @Override
    public List<PublicRoute> publicRoutes() {
        return List.of(
                new PublicRoute(HttpMethod.GET, "/api/invitations/*/preview"),
                new PublicRoute(HttpMethod.POST, "/api/invitations/*/accept")
        );
    }
}
