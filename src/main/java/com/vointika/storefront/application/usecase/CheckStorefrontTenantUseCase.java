package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontTenantQuery;

/**
 * Is there a storefront at this host? The only read the placeholder makes.
 *
 * <p>It exists as a use case rather than the controller calling the port
 * directly because the layer DAG says so — {@code presentation} depends on
 * {@code application}, and the port is reached from there. That it is one line
 * is the point: everything else this context used to ask for is gone until the
 * real pages return.
 */
public class CheckStorefrontTenantUseCase {

    private final StorefrontTenantQuery storefrontTenantQuery;

    public CheckStorefrontTenantUseCase(StorefrontTenantQuery storefrontTenantQuery) {
        this.storefrontTenantQuery = storefrontTenantQuery;
    }

    public boolean execute(String handle) {
        return storefrontTenantQuery.exists(handle);
    }
}
