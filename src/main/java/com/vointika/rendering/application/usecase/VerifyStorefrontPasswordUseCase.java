package com.vointika.rendering.application.usecase;

import com.vointika.shared.port.StorefrontOperatorQuery;

/**
 * Checks a visitor's storefront password against the operator's gate.
 *
 * <p>Answers with a boolean and never throws for a wrong password or an unknown
 * tenant: the two are indistinguishable by design, so the gate cannot be used to
 * enumerate which handles exist. A failed attempt is not an error condition — it
 * is the expected outcome of a guess — so the transport stays 200 and only the
 * body differs (a 401 here would also collide with the shared-secret filter's
 * own 401, which means something entirely different).
 *
 * <p>The unlock itself is the BFF's: it sets the signed cookie that lets
 * subsequent page requests through. This use case only answers the question.
 */
public class VerifyStorefrontPasswordUseCase {

    private final StorefrontOperatorQuery storefrontOperatorQuery;

    public VerifyStorefrontPasswordUseCase(StorefrontOperatorQuery storefrontOperatorQuery) {
        this.storefrontOperatorQuery = storefrontOperatorQuery;
    }

    public boolean execute(String handle, String candidate) {
        return storefrontOperatorQuery.verifyStorefrontPassword(handle, candidate);
    }
}
