package com.vointika.storefront.application.policy;

import java.util.List;

/**
 * Every address the storefront answers on, defined once because <b>three
 * registries have to agree on each of them</b>: the {@code @GetMapping}, the
 * {@code PublicRoute} entries (GET <em>and</em> HEAD), and the lock
 * interceptor's patterns. Miss one and nothing fails on its own to say so — a
 * missing {@code PublicRoute} is a 401, a missing interceptor pattern serves the
 * page ungated on a locked store.
 *
 * <p>It lives in {@code application} because {@code presentation} and
 * {@code infrastructure} may not see each other and both consumers sit in one of
 * the two (PATTERNS §1).
 *
 * <p><b>These addresses no longer all serve the same thing.</b> {@link #HOME} and
 * {@link #LOCALE} serve the globals contract; {@link #PAGE} and
 * {@link #LOCALIZED_PAGE} serve a CMS page; only the experiences and policies
 * routes still answer the {@code {handle,status}} placeholder. The addresses were
 * always the expensive part to get right — the lessons are recorded in
 * {@code StorefrontPublicRoutes} — and they are what the remaining pages hang off.
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

    /**
     * The namespace a CMS page lives under. There is no index at {@code /pages}
     * itself — {@link #PAGE} is the route, and it is built from this — so nothing
     * registers this constant on its own.
     */
    public static final String PAGES = "/pages";

    /**
     * One CMS page. Constrained to the {@code Handle} shape for the reason every
     * pattern here is constrained: this is a {@code PublicRoute} pattern before
     * it is a route. Non-capturing, because {@code PathPatternParser} refuses a
     * capture group outright.
     */
    /**
     * One experience, at the handle the rendered locale publishes. Same constraint
     * as {@link #PAGE} — and it is a security pattern before it is a route, so the
     * group stays non-capturing: {@code PathPatternParser} rejects capture groups
     * in a constraint outright.
     *
     * <p>Unlike {@link #PAGES}, {@link #EXPERIENCES} is itself a live route (the
     * listing), so both it and this stay in {@link #PAGE_ROUTES}.
     */
    public static final String EXPERIENCE = EXPERIENCES + "/{handle:[a-z0-9]+(?:-[a-z0-9]+)*}";

    public static final String LOCALIZED_EXPERIENCE = LOCALE + EXPERIENCE;

    public static final String PAGE = PAGES + "/{handle:[a-z0-9]+(?:-[a-z0-9]+)*}";

    public static final String LOCALIZED_PAGE = LOCALE + PAGE;

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

    /**
     * The gate. <b>Not locale-prefixed, and it never will be</b> — the gate runs
     * before locale resolution, so it has no locale to be in; localizing it by
     * path would mean reading the path locale first, which is the leak the
     * ordering exists to prevent.
     *
     * <p><b>{@link #LOCALE} also matches this literal.</b> {@code PathPattern}
     * prefers the literal, so it works — but every top-level literal route added
     * later inherits the same collision, and it is asserted rather than assumed.
     */
    public static final String PASSWORD = "/password";

    /**
     * Every address that serves a <b>page</b>, and the reason this list exists
     * rather than three hand-kept copies of it.
     *
     * <p>A page route has to be registered in four places — its
     * {@code @GetMapping}, a {@code PublicRoute} for GET, another for HEAD, and
     * the lock interceptor's patterns — and until 2026-08-14 each was written out
     * by hand. Three of the four agreeing is a state nothing detected: the page
     * answers, so every test and every curl passes, while a store the operator
     * locked serves it to anyone. The registries now derive from this list, so
     * the only way to add a route to some-but-not-all is to edit them apart on
     * purpose.
     *
     * <p><b>{@link #PASSWORD} is deliberately not here.</b> It is public and it is
     * a route, but gating the gate would redirect it to itself; it is registered
     * on its own beside this list, and {@code StorefrontRouteRegistriesTest}
     * knows it as the one declared exception.
     */
    public static final List<String> PAGE_ROUTES = List.of(
            HOME, LOCALE,
            EXPERIENCES, LOCALIZED_EXPERIENCES,
            EXPERIENCE, LOCALIZED_EXPERIENCE,
            POLICY, LOCALIZED_POLICY,
            PAGE, LOCALIZED_PAGE);

    private StorefrontRoutes() {
    }
}
