package com.vointika.rendering.application.service;

import com.vointika.rendering.application.dto.output.NavigationItem;
import com.vointika.rendering.application.dto.output.NavigationMenu;
import com.vointika.shared.port.NavigationItemView;
import com.vointika.shared.port.NavigationMenuView;
import com.vointika.shared.port.StorefrontExperienceQuery;
import com.vointika.shared.port.StorefrontNavigationQuery;
import com.vointika.shared.port.StorefrontPageQuery;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Turns the operator's menus into something a storefront can render.
 *
 * <p>This is the one place three contexts meet, and it is why it lives in
 * {@code rendering}: menus belong to {@code touroperator}, the experiences and
 * pages they point at belong elsewhere, and no context may import another. Each
 * arrives through its own shared port and is joined here.
 *
 * <p><strong>An item whose target cannot be resolved is dropped.</strong> A menu
 * entry pointing at a draft, a deleted experience or another tenant's page would
 * otherwise render as a link to a 404 — worse than the entry simply not being
 * there. Resolution is batched: two queries for the whole navigation tree,
 * regardless of size, because this renders on every page.
 */
public class NavigationAssembler {

    private static final String LINK_EXPERIENCE = "EXPERIENCE";
    private static final String LINK_PAGE = "PAGE";

    private final StorefrontNavigationQuery navigationQuery;
    private final StorefrontExperienceQuery experienceQuery;
    private final StorefrontPageQuery pageQuery;

    public NavigationAssembler(StorefrontNavigationQuery navigationQuery,
                               StorefrontExperienceQuery experienceQuery,
                               StorefrontPageQuery pageQuery) {
        this.navigationQuery = navigationQuery;
        this.experienceQuery = experienceQuery;
        this.pageQuery = pageQuery;
    }

    public List<NavigationMenu> assemble(UUID tourOperatorId, String locale) {
        List<NavigationMenuView> menus = navigationQuery.findMenus(tourOperatorId, locale);
        if (menus.isEmpty()) {
            return List.of();
        }

        Set<UUID> experienceIds = new HashSet<>();
        Set<UUID> pageIds = new HashSet<>();
        for (NavigationMenuView menu : menus) {
            collectTargets(menu.items(), experienceIds, pageIds);
        }

        Map<UUID, String> experienceHandles =
                experienceQuery.publishedHandles(tourOperatorId, experienceIds, locale);
        Map<UUID, String> pageHandles =
                pageQuery.publishedHandles(tourOperatorId, pageIds, locale);

        return menus.stream()
                .map(menu -> new NavigationMenu(
                        menu.handle(),
                        menu.title(),
                        resolve(menu.items(), experienceHandles, pageHandles)))
                .toList();
    }

    private void collectTargets(List<NavigationItemView> items, Set<UUID> experienceIds, Set<UUID> pageIds) {
        for (NavigationItemView item : items) {
            if (item.resourceId() != null) {
                if (LINK_EXPERIENCE.equals(item.linkType())) {
                    experienceIds.add(item.resourceId());
                } else if (LINK_PAGE.equals(item.linkType())) {
                    pageIds.add(item.resourceId());
                }
            }
            collectTargets(item.children(), experienceIds, pageIds);
        }
    }

    private List<NavigationItem> resolve(List<NavigationItemView> items,
                                         Map<UUID, String> experienceHandles,
                                         Map<UUID, String> pageHandles) {
        List<NavigationItem> resolved = new ArrayList<>();
        for (NavigationItemView item : items) {
            String handle = handleFor(item, experienceHandles, pageHandles);

            // A resource-targeted item with no resolvable handle points at
            // something the public cannot see. Drop it, and its children with
            // it — a submenu hanging off a dead parent has nothing to hang from.
            if (needsHandle(item) && handle == null) {
                continue;
            }

            resolved.add(new NavigationItem(
                    item.title(),
                    item.linkType(),
                    handle,
                    item.externalUrl(),
                    resolve(item.children(), experienceHandles, pageHandles)));
        }
        return List.copyOf(resolved);
    }

    private boolean needsHandle(NavigationItemView item) {
        return LINK_EXPERIENCE.equals(item.linkType()) || LINK_PAGE.equals(item.linkType());
    }

    private String handleFor(NavigationItemView item,
                             Map<UUID, String> experienceHandles,
                             Map<UUID, String> pageHandles) {
        if (item.resourceId() == null) {
            return null;
        }
        if (LINK_EXPERIENCE.equals(item.linkType())) {
            return experienceHandles.get(item.resourceId());
        }
        if (LINK_PAGE.equals(item.linkType())) {
            return pageHandles.get(item.resourceId());
        }
        return null;
    }
}
