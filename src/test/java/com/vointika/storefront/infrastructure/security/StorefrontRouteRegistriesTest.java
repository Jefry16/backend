package com.vointika.storefront.infrastructure.security;

import com.vointika.shared.web.security.PublicRoute;
import com.vointika.storefront.application.policy.StorefrontRoutes;
import com.vointika.storefront.infrastructure.web.StorefrontWebConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * A storefront page route is registered in four places — its {@code @GetMapping},
 * a {@code PublicRoute} for GET, another for HEAD, and the lock interceptor's
 * patterns. <b>Three of the four agreeing is a state nothing detected.</b> The
 * page answers, so every test and every curl passes, while a store the operator
 * locked serves that page to anyone: exactly the leak the gate exists to prevent.
 *
 * <p>The registries now derive from {@link StorefrontRoutes#PAGE_ROUTES}, so they
 * cannot drift from each other. What is left to check is the thing derivation
 * cannot fix — that a <b>new constant</b> was added to the list at all, and that
 * the interceptor really received it.
 *
 * <p>{@link StorefrontRoutes#PASSWORD} is the one declared exception: public, and
 * deliberately ungated, because redirecting the gate to itself is a loop.
 * {@code PAGES} and {@code POLICIES} are namespace prefixes with no route of their
 * own — {@code PAGES} is the {@code /pages} segment {@code PAGE} is built from.
 */
class StorefrontRouteRegistriesTest {

    /** Constants that are deliberately not page routes, each for a stated reason. */
    private static final Set<String> NOT_A_PAGE_ROUTE = Set.of(
            "PASSWORD",  // public but ungated — gating the gate loops
            "PAGES",     // a namespace prefix, not an address
            "POLICIES"); // likewise: no index page at /policies

    /**
     * The check that survives derivation. Add a constant, forget the list, and
     * every registry silently skips it — this is what says so.
     */
    @Test
    void everyRouteConstantIsEitherAPageRouteOrADeclaredException() throws Exception {
        List<String> unlisted = new ArrayList<>();
        int examined = 0;

        for (Field f : StorefrontRoutes.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != String.class) {
                continue;
            }
            examined++;
            String value = (String) f.get(null);
            if (!StorefrontRoutes.PAGE_ROUTES.contains(value) && !NOT_A_PAGE_ROUTE.contains(f.getName())) {
                unlisted.add(f.getName());
            }
        }

        assertThat(examined)
                .withFailMessage("No String constants found on StorefrontRoutes — the reflection "
                        + "broke, and a scan that examines nothing checks nothing.")
                .isGreaterThan(5);
        assertThat(unlisted)
                .withFailMessage("These route constants are in neither PAGE_ROUTES nor the declared "
                        + "exception set: %s%nA page route missing from PAGE_ROUTES is served "
                        + "UNGATED — a locked store hands it to anonymous visitors. Add it to the "
                        + "list, or to NOT_A_PAGE_ROUTE with a reason.", unlisted)
                .isEmpty();
    }

    /** Both methods, because Spring Security matches the exact one and 401s the rest. */
    @Test
    void everyPageRouteIsPublicForGetAndHead() {
        List<PublicRoute> routes = new StorefrontPublicRoutes().publicRoutes();

        for (String page : StorefrontRoutes.PAGE_ROUTES) {
            assertThat(routes)
                    .withFailMessage("%s is not public for GET", page)
                    .contains(new PublicRoute(HttpMethod.GET, page));
            assertThat(routes)
                    .withFailMessage("%s is not public for HEAD — Spring MVC serves HEAD from a "
                            + "@GetMapping for free, Spring Security does not, so this is a 401 "
                            + "the handler never sees.", page)
                    .contains(new PublicRoute(HttpMethod.HEAD, page));
        }
    }

    /**
     * The gate's own address is public on all three methods and <b>absent</b> from
     * the gated set. Both halves matter: drop the POST and the form 401s at the
     * filter chain; gate the gate and it redirects to itself.
     */
    @Test
    void thePasswordGateIsPublicAndNotItselfGated() {
        List<PublicRoute> routes = new StorefrontPublicRoutes().publicRoutes();

        assertThat(routes).contains(
                new PublicRoute(HttpMethod.GET, StorefrontRoutes.PASSWORD),
                new PublicRoute(HttpMethod.HEAD, StorefrontRoutes.PASSWORD),
                new PublicRoute(HttpMethod.POST, StorefrontRoutes.PASSWORD));
        assertThat(StorefrontRoutes.PAGE_ROUTES).doesNotContain(StorefrontRoutes.PASSWORD);
    }

    /**
     * Reads what the interceptor was actually handed, rather than trusting that
     * the config calls the right accessor. {@code InterceptorRegistry} exposes its
     * registrations only to {@code WebMvcConfigurationSupport}, so the
     * {@code getInterceptors} lookup is reflective — and asserted non-empty, or a
     * silent API change here would make this test pass by examining nothing.
     */
    @Test
    void theLockInterceptorGuardsEveryPageRouteAndOnlyThose() throws Exception {
        InterceptorRegistry registry = new InterceptorRegistry();
        new StorefrontWebConfig(providerOf(null), providerOf(null)).addInterceptors(registry);

        Method getInterceptors = InterceptorRegistry.class.getDeclaredMethod("getInterceptors");
        getInterceptors.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<Object> interceptors = (List<Object>) getInterceptors.invoke(registry);

        assertThat(interceptors)
                .withFailMessage("No interceptor registered — the reflective read broke.")
                .hasSize(1);

        MappedInterceptor mapped = (MappedInterceptor) interceptors.getFirst();

        assertThat(mapped.getIncludePathPatterns())
                .withFailMessage("The lock interceptor's patterns are not exactly PAGE_ROUTES. "
                        + "A page route missing here is served ungated on a locked store; an extra "
                        + "one gates something that should not be.")
                .containsExactlyInAnyOrderElementsOf(StorefrontRoutes.PAGE_ROUTES);
    }

    /** The gate's brute-force limit must track the route, not a copy of its path. */
    @Test
    void theRateLimitedPathIsThePasswordRouteItself() {
        var rules = new StorefrontRateLimitRoutes().rateLimitRules();

        assertThat(rules)
                .withFailMessage("The password POST is not rate limited at its own constant. A "
                        + "hardcoded copy stops matching the day the route is renamed, and what "
                        + "goes with it is the only brute-force limit on a store's password.")
                .anySatisfy(rule -> {
                    assertThat(rule.method()).isEqualTo(HttpMethod.POST);
                    assertThat(rule.pathPattern()).isEqualTo(StorefrontRoutes.PASSWORD);
                });
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> providerOf(T value) {
        return (ObjectProvider<T>) mock(ObjectProvider.class);
    }
}
