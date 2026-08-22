package com.vointika.storefront.infrastructure.security;

import com.vointika.shared.web.security.PublicRoute;
import com.vointika.shared.web.security.PublicRouteRegistrar;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The storefront is unauthenticated, and that is all it needs for now — not a
 * host-matched filter chain of its own. Nothing today wants storefront requests
 * to have <em>different</em> security, only <em>no</em> authentication:
 * {@code JwtAuthenticationFilter} leaves anonymous requests alone and
 * {@code ApiRateLimitFilter} passes through anything without a JWT principal.
 * Revisit when the storefront needs its own CORS, error shape or rate limit.
 *
 * <p><b>Every page route needs HEAD as well as GET, and has to say so.</b> Spring
 * MVC serves HEAD from a {@code @GetMapping} for free, but Spring Security
 * matches on the exact method — so a GET-only entry rejects HEAD at the filter
 * chain before MVC sees it: a <b>401</b>, not a 405, in the JSON shape
 * {@code RestAuthenticationEntryPoint} writes. Wrong for a public page. RFC 9110
 * has HEAD identical to GET minus the body, and HEAD is what crawlers, link
 * checkers, uptime monitors and CDNs send. Found by running it against the built
 * stack, after the home slice's tests and curls all used GET.
 *
 * <p>OPTIONS stays closed, deliberately: nothing preflights a server-rendered
 * same-origin page, so opening it would be a decision rather than a correction.
 *
 * <p>The {@code /password} entries below are <b>live</b>, on GET, HEAD and POST —
 * the gate cannot authenticate the request that is there to unlock it. Deleting
 * them 401s the gate at the filter chain, which is why this says so next to them
 * rather than leaving the reader to infer it.
 *
 * <p><b>Every pattern here is a security pattern first and a route second.</b>
 * The locale entry uses {@link StorefrontRoutes#LOCALE}, whose variable is
 * constrained, because a bare {@code /{locale}} would {@code permitAll} every
 * single-segment path in the application — {@code /error} today, and whatever
 * {@code /health} or {@code /metrics} arrives later, with nothing failing to say
 * so. Pinned by {@code LocalePathTemplateTest}, which asserts the constraint
 * matches every seeded language code and <b>none</b> of the storefront's own
 * literal segments — {@code experiences}, {@code password} — so a route cannot
 * quietly become a locale.
 *
 * <p>This line named {@code StorefrontPlaceholderControllerTest} until that class
 * was deleted with the policy page, and it was wrong before then: that test used
 * these routes but asserted nothing about the constraint.
 */
@Component
public class StorefrontPublicRoutes implements PublicRouteRegistrar {

    @Override
    public List<PublicRoute> publicRoutes() {
        List<PublicRoute> routes = new ArrayList<>();
        // GET and HEAD for every page: Spring MVC serves HEAD from a @GetMapping
        // for free, Spring Security does not, so a GET-only entry rejects HEAD at
        // the filter chain as a 401 the handler never sees. Crawlers, link
        // checkers and uptime monitors all send HEAD.
        for (String page : StorefrontRoutes.PAGE_ROUTES) {
            routes.add(new PublicRoute(HttpMethod.GET, page));
            routes.add(new PublicRoute(HttpMethod.HEAD, page));
        }
        // The gate itself, which is not in PAGE_ROUTES because it must not be
        // gated. POST is the submission; without it the form would 401 at the
        // filter chain and never reach the controller.
        routes.add(new PublicRoute(HttpMethod.GET, StorefrontRoutes.PASSWORD));
        routes.add(new PublicRoute(HttpMethod.HEAD, StorefrontRoutes.PASSWORD));
        routes.add(new PublicRoute(HttpMethod.POST, StorefrontRoutes.PASSWORD));
        return List.copyOf(routes);
    }
}
