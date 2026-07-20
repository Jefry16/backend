package com.vointika.shared.web.security;

import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The published REST Docs API guide (served from {@code static/docs/}) is public
 * — it's developer documentation (endpoint shapes only), readable without a token.
 */
@Component
public class DocsPublicRoutes implements PublicRouteRegistrar {

    @Override
    public List<PublicRoute> publicRoutes() {
        return List.of(
                new PublicRoute(HttpMethod.GET, "/docs/**")
        );
    }
}
