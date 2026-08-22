package com.vointika.storefront.infrastructure.security;

import com.vointika.shared.web.security.UnauthenticatedRequestPolicy;
import com.vointika.storefront.application.policy.StorefrontNotFound;
import com.vointika.storefront.application.policy.TenantHandleResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * On a tenant's host there is nothing to authenticate <em>to</em>, so a request
 * that matches no route is a 404 rather than a 401.
 *
 * <p><b>Every storefront route constrains its path variables</b>, which is a
 * security decision and stays one (see {@code StorefrontPublicRoutes}). The cost
 * is that a visitor's typo — {@code /policies/legal_notice},
 * {@code /pages/About_Us} — matches no {@code PublicRoute}, never reaches MVC,
 * and used to be told {@code 401 Authentication required} by a site with no
 * login. Wrong for a visitor, and a small leak: it says an auth system exists to
 * fail against.
 *
 * <p><b>Host, not path prefix.</b> On a host that resolves to a tenant, every
 * path is the storefront's — that is what a tenant host <em>is</em> — so this
 * needs no list of namespaces to keep in step with the routes. A list would be
 * the allowlist written twice, and the second copy is the one that goes stale.
 *
 * <p><b>{@code /api} keeps its 401, deliberately.</b> The admin API is not served
 * on tenant hosts in any deployment we intend, so this is not protecting a live
 * path — it is protecting <em>debugging</em>. An expired token against
 * {@code acme.localhost:8080/api/...} in dev should say the token is the problem;
 * "There is no storefront at this address" would send the reader looking for a
 * routing bug that is not there. It is the one place where the two surfaces
 * overlap on one origin, which is exactly the dev stack.
 *
 * <p><b>The apex host is untouched and still answers 401.</b> {@code localhost}
 * resolves to no tenant, so nothing there is claimed. That is a real asymmetry —
 * {@code localhost:8080/pages/about-us} 404s (it matches a route, then finds no
 * operator) while {@code /pages/About_Us} 401s — and it is left alone because the
 * apex serves the admin API, where a 401 is the honest answer.
 *
 * <p><b>It cannot swallow a server error, and that is structural rather than
 * lucky.</b> This runs only from {@code AuthenticationEntryPoint.commence}, which
 * Spring Security invokes only when authentication has failed — a 500 never
 * reaches it, and neither does anything MVC handled. The obvious worry is
 * {@code /error}: a direct request to it on a tenant host does now answer the
 * storefront's 404 (it did answer 401 before, which was no better), while a
 * genuine failure still renders its own 500. Verified live rather than reasoned:
 * a malformed cursor on {@code /experiences} answers
 * {@code 500 "An unexpected error occurred"} on a tenant host, unchanged.
 *
 * <p>{@code ObjectProvider} for the same reason {@code StorefrontLockInterceptor}
 * uses one: a {@code @WebMvcTest} slice loads the security config without the
 * storefront's own beans, and this must not fail the context in that case. No
 * resolver means no tenant means no claim — the 401 every other context expects.
 */
@Component
public class StorefrontUnauthenticatedRequests implements UnauthenticatedRequestPolicy {

    private static final String ADMIN_API_PREFIX = "/api/";

    private final ObjectProvider<TenantHandleResolver> tenantHandleResolver;

    public StorefrontUnauthenticatedRequests(ObjectProvider<TenantHandleResolver> tenantHandleResolver) {
        this.tenantHandleResolver = tenantHandleResolver;
    }

    @Override
    public Optional<String> notFoundMessage(HttpServletRequest request) {
        if (request.getRequestURI().startsWith(ADMIN_API_PREFIX)) {
            return Optional.empty();
        }
        return Optional.ofNullable(tenantHandleResolver.getIfAvailable())
                .flatMap(resolver -> resolver.resolve(request.getServerName()))
                .map(handle -> StorefrontNotFound.MESSAGE);
    }
}
