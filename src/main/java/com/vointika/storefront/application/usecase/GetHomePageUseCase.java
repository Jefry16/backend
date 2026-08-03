package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.storefront.application.dto.output.HomePageOutput;

import java.util.Optional;

/**
 * Loads the shop a handle addresses, or nothing. Empty means "no such
 * storefront" — the caller answers 404; this returns no exception, because the
 * miss is ordinary traffic on a public site, not a fault.
 *
 * <p>Thin on purpose. Every other context puts orchestration in a use case, and
 * the storefront's first slice should not set the precedent of a controller
 * calling a query port directly — the SEO title fallback below is already
 * policy, and there will be more.
 */
public class GetHomePageUseCase {

    private final StorefrontShopQuery storefrontShopQuery;

    public GetHomePageUseCase(StorefrontShopQuery storefrontShopQuery) {
        this.storefrontShopQuery = storefrontShopQuery;
    }

    public Optional<HomePageOutput> execute(String handle) {
        return storefrontShopQuery.findByHandle(handle).map(GetHomePageUseCase::toOutput);
    }

    private static HomePageOutput toOutput(StorefrontShopQuery.StorefrontShopView shop) {
        return new HomePageOutput(
                shop.seoTitle() == null ? shop.name() : shop.seoTitle(),
                shop.name(),
                shop.seoDescription(),
                shop.logoKey(),
                shop.ogImageKey());
    }
}
