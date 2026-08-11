package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;

/**
 * Is there a storefront at this host? The only read the routes that are still
 * placeholders make.
 *
 * <p>It exists as a use case rather than the controller calling the port
 * directly because the layer DAG says so — {@code presentation} depends on
 * {@code application}, and the port is reached from there.
 *
 * <p>It asks {@code findLocales} rather than a dedicated {@code exists}, because
 * a second port answering the same question is a second thing to keep true. The
 * locales read is one row, and this caller throws away everything but its
 * presence.
 */
public class CheckStorefrontTenantUseCase {

    private final StorefrontShopQuery storefrontShopQuery;

    public CheckStorefrontTenantUseCase(StorefrontShopQuery storefrontShopQuery) {
        this.storefrontShopQuery = storefrontShopQuery;
    }

    public boolean execute(String handle) {
        return storefrontShopQuery.findLocales(handle).isPresent();
    }
}
