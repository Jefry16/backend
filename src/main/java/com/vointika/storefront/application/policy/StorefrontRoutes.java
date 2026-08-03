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
 * <p>Only the experiences listing is here. {@code /} and {@code /password} are
 * still literals repeated in their three places, from before this file existed;
 * moving them is a change to routes this slice does not otherwise touch.
 */
public final class StorefrontRoutes {

    public static final String EXPERIENCES = "/experiences";

    /**
     * <b>Built from the locale template, never retyped.</b> The template's
     * constraint is a security decision (see {@link LocaleResolver#PATH_TEMPLATE});
     * a second hand-written copy of the regex is a second thing to keep true.
     */
    public static final String LOCALIZED_EXPERIENCES = LocaleResolver.PATH_TEMPLATE + EXPERIENCES;

    private StorefrontRoutes() {
    }
}
