package com.vointika.storefront.application.policy;

/**
 * What every storefront miss says, in one place.
 *
 * <p>An unknown operator, a locale the operator does not publish, a handle
 * nothing answers to, a draft, and a path that is not a storefront route at all
 * are <b>deliberately indistinguishable</b>. That only holds while they say the
 * same words, and they are now produced in two different layers —
 * {@code presentation} raises it as an exception from a controller,
 * {@code infrastructure} writes it straight to the response when Spring Security
 * rejects a request before MVC sees it.
 *
 * <p><b>Which is exactly why the constant lives here.</b> Those two layers may not
 * import each other (PATTERNS §1), so a constant in either of them means a copy
 * in the other — and a copy is one edit away from telling a visitor which kind of
 * miss they hit. {@code application} is the only place both can reach.
 */
public final class StorefrontNotFound {

    public static final String MESSAGE = "There is no storefront at this address";

    private StorefrontNotFound() {
    }
}
