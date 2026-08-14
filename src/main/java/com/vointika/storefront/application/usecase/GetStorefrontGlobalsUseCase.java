package com.vointika.storefront.application.usecase;

import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontMenuQuery;
import com.vointika.shared.port.StorefrontMenuQuery.MenuItemView;
import com.vointika.shared.port.StorefrontPageQuery;
import com.vointika.shared.port.StorefrontMetafieldQuery;
import com.vointika.shared.port.StorefrontTourOperatorQuery;
import com.vointika.shared.port.StorefrontTourOperatorQuery.LocalesView;
import com.vointika.shared.port.StorefrontTourOperatorQuery.TourOperatorView;
import com.vointika.storefront.application.dto.output.MenuData;
import com.vointika.storefront.application.dto.output.StorefrontGlobals;
import com.vointika.storefront.application.dto.output.StorefrontGlobals.LocalizationData;
import com.vointika.storefront.application.policy.LocaleRule;
import com.vointika.storefront.application.policy.MenuTree;
import com.vointika.storefront.application.policy.SeoText;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Assembles the objects every storefront address carries. <b>Every route calls
 * this one</b> — that is what keeps {@code operator.brand} from meaning something
 * different on the home page than on an experience page.
 *
 * <p>Two reads, in the order the request forces: the tenant's locales decide
 * which locale is being served, and only then can the operator be read with its
 * translations overlaid.
 *
 * <p><b>Empty means 404, for either reason.</b> An unknown handle and a locale
 * the operator does not publish give the same answer on purpose — telling them apart
 * tells an anonymous visitor which operators exist and which languages they have.
 */
public class GetStorefrontGlobalsUseCase {

    private final StorefrontTourOperatorQuery operatorQuery;
    private final StorefrontMetafieldQuery metafieldQuery;
    private final StorefrontExperienceQuery experienceQuery;
    private final StorefrontMenuQuery menuQuery;
    private final StorefrontPageQuery pageQuery;

    public GetStorefrontGlobalsUseCase(StorefrontTourOperatorQuery operatorQuery,
                                       StorefrontMetafieldQuery metafieldQuery,
                                       StorefrontExperienceQuery experienceQuery,
                                       StorefrontMenuQuery menuQuery,
                                       StorefrontPageQuery pageQuery) {
        this.operatorQuery = operatorQuery;
        this.metafieldQuery = metafieldQuery;
        this.experienceQuery = experienceQuery;
        this.menuQuery = menuQuery;
        this.pageQuery = pageQuery;
    }

    /**
     * @param pathLocale the locale segment of the URL, or null for the bare path
     */
    public Optional<StorefrontGlobals> execute(String handle, String pathLocale) {
        return operatorQuery.findLocales(handle).flatMap(locales ->
                LocaleRule.resolve(pathLocale, locales.primaryLocale(), locales.supportedLocales())
                        .flatMap(locale -> operatorQuery.findOperator(locales.tourOperatorId(), locale)
                                .map(operator -> globals(operator, locale, locales,
                                        metafieldQuery.findForOperator(locales.tourOperatorId(), locale),
                                        experienceQuery.findFeatured(locales.tourOperatorId(), locale),
                                        menus(locales.tourOperatorId(), locale)))));
    }

    private static StorefrontGlobals globals(TourOperatorView operator, String locale, LocalesView locales,
                                             List<StorefrontMetafieldQuery.MetafieldView> metafields,
                                             List<StorefrontExperienceQuery.ExperienceCardView> featured,
                                             List<MenuData> menus) {
        return new StorefrontGlobals(
                operator,
                // The home page has no object of its own, so the operator is the whole
                // chain. A page type with its own SEO substitutes it (withSeo).
                SeoText.title(null, null, operator.seoTitle(), operator.name()),
                SeoText.description(null, operator.seoDescription()),
                operator.ogImageMediaId(),
                metafields,
                featured,
                menus,
                new LocalizationData(locale, locales.primaryLocale(),
                        primaryFirst(locales.primaryLocale(), locales.supportedLocales())));
    }

    /**
     * <b>Two batch lookups for the whole navigation, whatever it holds</b> — the
     * ids are collected across every menu first, so a header and a footer that
     * both link to the same page cost one read between them, not one each.
     */
    private List<MenuData> menus(UUID tourOperatorId, String locale) {
        List<StorefrontMenuQuery.MenuView> menus = menuQuery.findMenus(tourOperatorId, locale);
        if (menus.isEmpty()) {
            return List.of();
        }

        Set<UUID> experienceIds = new LinkedHashSet<>();
        Set<UUID> pageIds = new LinkedHashSet<>();
        for (StorefrontMenuQuery.MenuView menu : menus) {
            for (MenuItemView item : menu.items()) {
                switch (item.linkType()) {
                    case "EXPERIENCE" -> experienceIds.add(item.resourceId());
                    case "PAGE" -> pageIds.add(item.resourceId());
                    default -> { }
                }
            }
        }

        Map<UUID, String> experienceHandles =
                experienceQuery.findPublishedHandles(tourOperatorId, experienceIds, locale);
        Map<UUID, String> pageHandles =
                pageQuery.findPublishedHandles(tourOperatorId, pageIds, locale);

        return menus.stream()
                .map(menu -> new MenuData(menu.handle(), menu.title(),
                        MenuTree.build(menu.items(), experienceHandles, pageHandles)))
                .toList();
    }

    private static List<String> primaryFirst(String primary, Set<String> supported) {
        List<String> ordered = new ArrayList<>(supported);
        ordered.remove(primary);
        ordered.sort(String::compareTo);
        ordered.add(0, primary);
        return List.copyOf(ordered);
    }
}
