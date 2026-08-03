package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontGateView;
import com.vointika.shared.port.StorefrontShopQuery.StorefrontShopView;
import com.vointika.storefront.application.dto.output.HomePageOutput;
import com.vointika.storefront.application.policy.LocaleResolver;

import java.util.Optional;

/**
 * Loads the shop a handle addresses, in the locale the path asks for, or
 * nothing. Empty means "no page here" — an unknown storefront, or a locale this
 * operator does not publish — and the caller answers 404. It throws nothing: a
 * miss is ordinary traffic on a public site, not a fault.
 *
 * <p>It reads the tenant in two stages on purpose. The gate row is what decides
 * <em>which</em> locale the page is in ({@link LocaleResolver}); only then can
 * the content be asked for with that locale's translations overlaid. Keeping
 * both stages here rather than in the controller puts the ordering in one place
 * and keeps the query port out of {@code presentation}.
 */
public class GetHomePageUseCase {

    private final StorefrontShopQuery storefrontShopQuery;

    public GetHomePageUseCase(StorefrontShopQuery storefrontShopQuery) {
        this.storefrontShopQuery = storefrontShopQuery;
    }

    /**
     * @param pathLocale the locale prefix in the URL, or {@code null} for a bare
     *                   {@code /}
     */
    public Optional<HomePageOutput> execute(String handle, String pathLocale) {
        return storefrontShopQuery.findGate(handle).flatMap(gate -> render(gate, pathLocale));
    }

    private Optional<HomePageOutput> render(StorefrontGateView gate, String pathLocale) {
        return LocaleResolver.resolve(pathLocale, gate.primaryLocale(), gate.supportedLocales())
                .flatMap(locale -> storefrontShopQuery.findContent(gate.tourOperatorId(), locale)
                        .map(shop -> toOutput(shop, locale)));
    }

    private static HomePageOutput toOutput(StorefrontShopView shop, String locale) {
        return new HomePageOutput(
                locale,
                shop.seoTitle() == null ? shop.name() : shop.seoTitle(),
                shop.name(),
                shop.seoDescription(),
                shop.logoKey(),
                shop.ogImageKey());
    }
}
