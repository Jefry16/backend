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
