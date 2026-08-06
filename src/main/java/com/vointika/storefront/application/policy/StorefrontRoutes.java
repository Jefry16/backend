package com.vointika.storefront.application.policy;

/**
 * The page routes that must not be retyped, because <b>three registries have to
 * agree on every one of them</b>: the {@code @GetMapping}, the
 * {@code PublicRoute} entries and the gate's interceptor patterns. Miss the
 * second and the page is a 401; miss the third and a locked store serves it.
 * Nothing fails on its own to say so.
 *
 * <p>It lives in {@code application} because {@code presentation} and
 * {@code infrastructure} may not see each other and all three consumers sit in
 * one of the two — the same reason {@link LocaleResolver#PATH_TEMPLATE} and the
 * unlock cookie's name live where they do (PATTERNS §1).
 *
 * <p>{@code /} and {@code /password} are still literals repeated in their three
 * places, from before this file existed; moving them is a change to routes no
 * slice since has otherwise touched.
 */
public final class StorefrontRoutes {

    public static final String EXPERIENCES = "/experiences";

    /**
     * <b>Built from the locale template, never retyped.</b> The template's
     * constraint is a security decision (see {@link LocaleResolver#PATH_TEMPLATE});
     * a second hand-written copy of the regex is a second thing to keep true.
     */
    public static final String LOCALIZED_EXPERIENCES = LocaleResolver.PATH_TEMPLATE + EXPERIENCES;

    /**
     * The namespace one policy lives under. <b>There is no page at
     * {@code /policies}</b> — a policy is addressed by its type and there is no
     * index listing them — so this is a prefix rather than a route, and nothing
     * registers it.
     */
    public static final String POLICIES = "/policies";

    /**
     * <b>The variable is constrained, as {@link LocaleResolver#PATH_TEMPLATE}'s
     * is.</b> The blast radius is smaller here — {@code /policies/*} is a
     * namespace we own outright, not every single-segment path — but this string
     * is a {@code PublicRoute} pattern before it is a route, and consistency is
     * cheaper than remembering the day it mattered. The constraint is also what
     * makes the slug ↔ type mapping one-to-one: {@code /policies/Terms} is a 404
     * at the router rather than a second address for the same page.
     *
     * <p>Non-capturing, because {@code PathPatternParser} rejects capture groups
     * in a constraint regex outright.
     */
    public static final String POLICY = POLICIES + "/{type:[a-z]+(?:-[a-z]+)*}";

    public static final String LOCALIZED_POLICY = LocaleResolver.PATH_TEMPLATE + POLICY;

    private StorefrontRoutes() {
    }
}
