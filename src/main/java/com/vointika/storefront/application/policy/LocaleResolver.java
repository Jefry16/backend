package com.vointika.storefront.application.policy;

import java.util.Optional;
import java.util.Set;

/**
 * The storefront's locale rule: <b>primary bare, supported secondaries prefixed,
 * everything else 404</b>.
 *
 * <table>
 *   <caption>What each request resolves to</caption>
 *   <tr><th>Request</th><th>Result</th></tr>
 *   <tr><td>{@code /}</td><td>the operator's primary locale</td></tr>
 *   <tr><td>{@code /{locale}}, supported and not primary</td><td>that locale</td></tr>
 *   <tr><td>{@code /{locale}} = primary</td><td>empty — 404</td></tr>
 *   <tr><td>{@code /{locale}} unsupported</td><td>empty — 404</td></tr>
 * </table>
 *
 * <p>The primary 404s under a prefix on purpose: it already lives at {@code /},
 * and two URLs serving one page is duplicate content. The comparison is exact
 * for the same reason — {@code /ES} is not a second address for {@code /es}.
 *
 * <p>V4 anticipates a per-locale {@code is_published} flag and does not have one,
 * so "published secondaries" is today every supported secondary.
 *
 * <p>A pure function of its three arguments, so it is static and no bean wires
 * it. It lives in {@code application/policy} for the same reason
 * {@link TenantHandleResolver} does: a storefront with no {@code domain} layer
 * has nowhere else to put a rule both {@code presentation} and
 * {@code infrastructure} may see.
 */
public final class LocaleResolver {

    /**
     * The URL template for a prefixed locale, <b>constrained on purpose</b>.
     *
     * <p>A bare {@code /{locale}} is a fine route and a terrible security
     * pattern: the same string goes into {@code PublicRoute}, so it would
     * {@code permitAll} <em>every</em> single-segment path — `/error`,
     * `/favicon.ico`, and whatever `/health` or `/metrics` somebody adds later,
     * silently and with nothing failing. Constraining the variable makes the
     * security pattern as narrow as the route.
     *
     * <p>It is also what keeps the gate off the container's `/error` dispatch:
     * the interceptor registers these same patterns, so a genuine 500 on a locked
     * store stays a 500 instead of being rewritten into a redirect to
     * `/password`. `/password` falls outside for the same reason and needs no
     * exclusion — eight letters do not match two.
     *
     * <p>The shape must stay at least as wide as {@code reference.languages},
     * which is today six two-letter lowercase codes; the optional group leaves
     * room for `pt-br` without a second visit. A code that does not match is a
     * 404 on a locale the operator publishes, which is why
     * {@code LocalePathTemplateTest} fails the build if the two ever diverge.
     *
     * <p><b>The group must be non-capturing.</b> {@code PathPatternParser} throws
     * {@code IllegalArgumentException: No capture groups allowed in the
     * constraint regex} at startup — it uses the group count to bind the path
     * variable — so {@code (-[a-z0-9]{2,4})?} is rejected and
     * {@code (?:-[a-z0-9]{2,4})?} is not. Nested braces are fine; only the group
     * is. Found by running it.
     *
     * <p>One constant, three consumers — the mapping, both `PublicRoute` entries
     * and the gate's patterns — and it lives here because `presentation` and
     * `infrastructure` may not see each other.
     */
    public static final String PATH_TEMPLATE = "/{locale:[a-z]{2}(?:-[a-z0-9]{2,4})?}";

    private LocaleResolver() {
    }

    /**
     * @param pathLocale the locale in the URL, or {@code null} for a bare {@code /}
     * @return the locale to render, or empty when the request addresses no page
     */
    public static Optional<String> resolve(String pathLocale, String primaryLocale, Set<String> supportedLocales) {
        if (pathLocale == null) {
            return Optional.of(primaryLocale);
        }
        if (pathLocale.equals(primaryLocale) || !supportedLocales.contains(pathLocale)) {
            return Optional.empty();
        }
        return Optional.of(pathLocale);
    }
}
