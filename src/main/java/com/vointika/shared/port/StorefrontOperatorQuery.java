package com.vointika.shared.port;

import java.util.Optional;

/**
 * Cross-context read of a tenant by its storefront handle — the entry point of
 * every public storefront request, which knows the operator only as the
 * subdomain label it was served on. Implemented in {@code touroperator}.
 *
 * <p>Password verification lives here rather than on the view so the stored
 * storefront password never leaves {@code touroperator}: callers ask whether a
 * candidate matches, and never see the secret itself.
 */
public interface StorefrontOperatorQuery {

    /** The operator holding this handle, or empty if no operator does. */
    Optional<StorefrontOperatorView> findByHandle(String handle);

    /**
     * Whether {@code candidate} unlocks this operator's storefront.
     *
     * <p>False whenever the operator does not exist, or the candidate does not
     * match — an unknown tenant is indistinguishable from a wrong password.
     * Also false when the gate is <em>off</em>: an operator with no password
     * enabled has nothing to unlock, and answering true would let a caller
     * confirm a handle exists.
     */
    boolean verifyStorefrontPassword(String handle, String candidate);
}
