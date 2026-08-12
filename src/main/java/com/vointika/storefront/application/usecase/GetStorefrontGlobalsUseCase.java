package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontMetafieldQuery;
import com.vointika.shared.port.StorefrontShopQuery;
import com.vointika.shared.port.StorefrontShopQuery.ShopLocalesView;
import com.vointika.shared.port.StorefrontShopQuery.ShopView;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontGlobals.LocalizationData;
import com.vointika.storefront.application.policy.LocaleRule;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Assembles the objects every storefront address carries. <b>Every route calls
 * this one</b> — that is what keeps {@code shop.brand} from meaning something
 * different on the home page than on an experience page.
 *
 * <p>Two reads, in the order the request forces: the tenant's locales decide
 * which locale is being served, and only then can the shop be read with its
 * translations overlaid.
 *
 * <p><b>Empty means 404, for either reason.</b> An unknown handle and a locale
 * the shop does not publish give the same answer on purpose — telling them apart
 * tells an anonymous visitor which shops exist and which languages they have.
 */
public class GetStorefrontGlobalsUseCase {

    private final StorefrontShopQuery shopQuery;
    private final StorefrontMetafieldQuery metafieldQuery;

    public GetStorefrontGlobalsUseCase(StorefrontShopQuery shopQuery,
                                       StorefrontMetafieldQuery metafieldQuery) {
        this.shopQuery = shopQuery;
        this.metafieldQuery = metafieldQuery;
    }

    /**
     * @param pathLocale the locale segment of the URL, or null for the bare path
     */
    public Optional<StorefrontGlobals> execute(String handle, String pathLocale) {
        return shopQuery.findLocales(handle).flatMap(locales ->
                LocaleRule.resolve(pathLocale, locales.primaryLocale(), locales.supportedLocales())
                        .flatMap(locale -> shopQuery.findShop(locales.tourOperatorId(), locale)
                                .map(shop -> globals(shop, locale, locales,
                                        metafieldQuery.findForOperator(locales.tourOperatorId())))));
    }

    private static StorefrontGlobals globals(ShopView shop, String locale, ShopLocalesView locales,
                                             List<StorefrontMetafieldQuery.MetafieldView> metafields) {
        return new StorefrontGlobals(
                shop,
                // The home page has no object of its own, so the shop IS the whole
                // fallback chain. A page type that carries its own SEO widens this,
                // and that is the slice to extract it in — not this one.
                shop.seoTitle() != null ? shop.seoTitle() : shop.name(),
                shop.seoDescription(),
                shop.ogImageMediaId(),
                metafields,
                new LocalizationData(locale, locales.primaryLocale(),
                        primaryFirst(locales.primaryLocale(), locales.supportedLocales())));
    }

    private static List<String> primaryFirst(String primary, Set<String> supported) {
        List<String> ordered = new ArrayList<>(supported);
        ordered.remove(primary);
        ordered.sort(String::compareTo);
        ordered.add(0, primary);
        return List.copyOf(ordered);
    }
}
