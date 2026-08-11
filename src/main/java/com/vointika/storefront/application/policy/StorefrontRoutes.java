package com.vointika.storefront.application.policy;

/**
 * Every address the storefront answers on, defined once because <b>two
 * registries have to agree on each of them</b>: the {@code @GetMapping} and the
 * {@code PublicRoute} entries. Miss the second and the page is a 401, and
 * nothing fails on its own to say so. (It was three registries until the
 * password gate's interceptor patterns went with the gate.)
 *
 * <p>It lives in {@code application} because {@code presentation} and
 * {@code infrastructure} may not see each other and both consumers sit in one of
 * the two (PATTERNS §1).
 *
 * <p><b>The routes outlived the pages they used to serve.</b> While the
 * storefront is a placeholder every one of these renders the same page — but the
 * addresses are the part that is expensive to get back right (four registries'
 * worth of lessons are recorded in {@code StorefrontPublicRoutes}), and they are
 * what the real pages will hang off.
 */
public final class StorefrontRoutes {

    public static final String HOME = "/";

    /**
     * <b>The variable is constrained, and that is a security decision rather
     * than a routing one.</b> A bare {@code /{locale}} makes a
     * {@code PublicRoute} that {@code permitAll}s <em>every single-segment path
     * in the application</em> — review of #91 measured {@code GET /error} going
     * 401 → 200, and whatever {@code /health} or {@code /metrics} lands later
     * inherits it silently.
     *
     * <p>The group <b>must be non-capturing</b>: {@code PathPatternParser} throws
     * {@code IllegalArgumentException: No capture groups allowed in the
     * constraint regex}. Nested {@code {2}} braces parse fine; only the group
     * does not.
     *
     * <p>It must stay at least as wide as {@code reference.languages}, or an
     * operator publishes a locale the storefront 404s with nothing failing —
     * pinned by {@code LocalePathTemplateTest}, which reads the real migrations.
     * <b>Which locales are actually published is no longer checked here</b>: that
     * needed the operator's locale list, which is a query, so the placeholder
     * accepts any locale-shaped segment and the rule returns with the pages.
     */
    public static final String LOCALE = "/{locale:[a-z]{2}(?:-[a-z0-9]{2,4})?}";

    public static final String EXPERIENCES = "/experiences";

    /** <b>Built from the locale template, never retyped</b> — a second copy of that regex is a second thing to keep true. */
    public static final String LOCALIZED_EXPERIENCES = LOCALE + EXPERIENCES;

    /**
     * The namespace one policy lives under. <b>There is no page at
     * {@code /policies}</b> — a policy is addressed by its type and there is no
     * index — so this is a prefix rather than a route, and nothing registers it.
     */
    public static final String POLICIES = "/policies";

    /**
     * Constrained like {@link #LOCALE}. Smaller blast radius — {@code /policies/*}
     * is a namespace we own outright — but this string is a {@code PublicRoute}
     * pattern before it is a route, and consistency is cheaper than remembering
     * the day it mattered. Non-capturing, for the same parser reason.
     */
    public static final String POLICY = POLICIES + "/{type:[a-z]+(?:-[a-z]+)*}";

    public static final String LOCALIZED_POLICY = LOCALE + POLICY;

    private StorefrontRoutes() {
    }
}
