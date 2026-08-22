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

    /**
     * <b>Every path variable in every route constant is constrained.</b>
     *
     * <p>These constants are {@code PublicRoute} patterns before they are routes,
     * so an unconstrained variable is not a routing looseness — it is a
     * {@code permitAll} over a whole shape of path. {@code CLAUDE.md} records what
     * that cost on {@code /{locale}}: a bare variable made <em>every single-segment
     * path in the application</em> public, {@code /error} measured going 401 → 200,
     * and whatever {@code /health} or {@code /metrics} lands later inheriting it
     * silently.
     *
     * <p><b>Written as an invariant because the per-constant version left a hole
     * for two PRs.</b> Three of the four were guarded, each by a different test and
     * each by accident of what that test needed: {@code LOCALE} by
     * {@code LocalePathTemplateTest}, {@code POLICY} by {@code PolicySlugTest},
     * {@code PAGE} behaviourally by
     * {@code StorefrontCmsPageControllerTest.aSegmentThatIsNotHandleShapedIsNotAPageRoute}.
     * {@code EXPERIENCE} had nothing: replacing its constraint with a bare
     * {@code {handle}} left <b>1450 tests, 0 failures</b> while both experience
     * routes began {@code permitAll}ing every path of that shape. Measured, then
     * reverted.
     *
     * <p>So this covers the constant nobody has written yet, which the four
     * hand-written guards could not.
     *
     * <p><b>Brace depth, not a regex.</b> {@code LOCALE} is
     * {@code {locale:[a-z]{2}(?:-[a-z0-9]{2,4})?}} — the constraint carries braces
     * of its own, so a non-greedy brace match stops at the first inner closing
     * brace and reads the variable as {@code locale:[a-z]} plus a stray {@code 2}.
     *
     * <p>The subtle part is that this still <em>passes</em>: the truncated text
     * contains a colon, so the verdict is right for the wrong reason. What breaks
     * is the <b>count</b> — and the count is what the anti-vacuity assertion below
     * hangs on. A parser that miscounts is a poor thing to base "did this scan
     * anything" on, which is what the extra ten lines buy.
     */
    @Test
    void everyPathVariableInEveryRouteConstantIsConstrained() throws Exception {
        List<String> unconstrained = new ArrayList<>();
        int variablesExamined = 0;

        for (Field f : StorefrontRoutes.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != String.class) {
                continue;
            }
            String template = (String) f.get(null);
            for (String variable : pathVariables(template)) {
                variablesExamined++;
                if (!variable.contains(":")) {
                    unconstrained.add(f.getName() + " -> {" + variable + "}");
                }
            }
        }

        // Every current constant taken together carries several variables. Zero
        // means the walk or the parser broke, and a scan that examines nothing
        // passes the assertion below without checking anything.
        assertThat(variablesExamined)
                .withFailMessage("Found no path variables across StorefrontRoutes, so this guard "
                        + "checked nothing. Fix the scan rather than deleting the test.")
                .isGreaterThan(3);

        assertThat(unconstrained)
                .withFailMessage("""
                        These route constants carry an unconstrained path variable:
                          %s
                        A route constant is a PublicRoute pattern before it is a route, so an \
                        unconstrained variable permitAlls every path of that shape — not just the \
                        addresses the route means to serve. Constrain it; do not widen the pattern \
                        to make this pass. Keep the group non-capturing: PathPatternParser rejects \
                        capture groups outright.""",
                        String.join("\n  ", unconstrained))
                .isEmpty();
    }

    /**
     * The {@code {...}} groups of a path template, brace-depth aware so a
     * constraint containing {@code {2,4}} is read whole.
     */
    private static List<String> pathVariables(String template) {
        List<String> variables = new ArrayList<>();
        int depth = 0;
        int start = -1;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    variables.add(template.substring(start + 1, i));
                }
            }
        }
        return variables;
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
