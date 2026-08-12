package com.vointika.storefront.application.dto.output;

import com.vointika.shared.port.StorefrontExperienceQuery.ExperienceCardView;
import com.vointika.shared.port.StorefrontMetafieldQuery.MetafieldView;
import com.vointika.shared.port.StorefrontShopQuery.ShopView;

import java.util.List;
import java.util.UUID;

/**
 * The objects every storefront address carries, whatever it renders.
 *
 * <p><b>This is the whole home page.</b> Shopify's index template has no object
 * of its own — a template gets the globals plus whatever object its type is
 * associated with, and {@code index} has none. So the globals payload and the
 * home page's payload are the same thing, and every later route is these objects
 * plus one more.
 *
 * <p><b>It carries the port's {@code ShopView} rather than a copy of it.</b> A
 * field-for-field duplicate would be an identical DTO pair with a mapper that
 * only copies — the shape MAP already carries as debt — and it would buy
 * nothing: the view is already a record of primitives with media as ids, which
 * is exactly what this layer deals in (PATTERNS §5). Presentation is where the
 * ids become URLs.
 *
 * @param ogImageMediaId still an id here, for that reason
 * @param metafields     flat here, nested by namespace in presentation — the
 *                       shape a theme addresses is a URL-side concern, and the
 *                       order they arrive in is the query's promise
 * @param menus          resolved and pruned: every link points somewhere that
 *                       exists, and the tree is the operator's arrangement
 * @param featuredExperiences bounded and ordered by the port, capped at
 *                       {@code FEATURED_LIMIT}: these ride <b>every</b> page, so
 *                       an unbounded list here is a payload a merchant grows by
 *                       ticking a checkbox
 */
public record StorefrontGlobals(ShopView shop,
                                String pageTitle,
                                String pageDescription,
                                UUID ogImageMediaId,
                                List<MetafieldView> metafields,
                                List<ExperienceCardView> featuredExperiences,
                                List<MenuData> menus,
                                LocalizationData localization) {

    /**
     * Locale <b>codes</b>, never URLs — a language's address is a route, and
     * routes are built in presentation.
     *
     * @param supported ordered primary-first, then alphabetically. The set comes
     *                  out of the database unordered and a language switcher that
     *                  reshuffles between requests is a bug report.
     */
    public record LocalizationData(String current, String primary, List<String> supported) {}

    /**
     * The same globals with the page's own SEO in place of the shop's defaults.
     *
     * <p>A page type that carries its own title and description substitutes them
     * here rather than the assembler branching per route — the globals are built
     * once, identically, and the one page-shaped difference is applied by the use
     * case that knows about pages.
     */
    public StorefrontGlobals withSeo(String newPageTitle, String newPageDescription) {
        return new StorefrontGlobals(shop, newPageTitle, newPageDescription, ogImageMediaId,
                metafields, featuredExperiences, menus, localization);
    }
}
