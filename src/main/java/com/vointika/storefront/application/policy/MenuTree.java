package com.vointika.storefront.application.policy;

import com.vointika.shared.port.StorefrontMenuQuery.MenuItemView;
import com.vointika.storefront.application.dto.output.MenuData.MenuLinkData;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns an operator's flat menu items into the tree a theme renders, and drops
 * every link that no longer points anywhere.
 *
 * <p><b>A link to an unpublished or deleted target is removed, not rendered.</b>
 * The operator arranged that menu when the target was live; unpublishing is how
 * they take something off the storefront, so leaving the link would defeat the
 * act — and a visitor would meet a 404 the operator never chose to show them.
 * Shopify's menus behave the same way. It is also why {@code page} and
 * {@code experience} were made to answer the publish question in one shape.
 *
 * <p><b>A dropped parent takes its children with it.</b> A child menu hangs off
 * its parent; promoting orphans into the top level would invent navigation the
 * operator never arranged.
 *
 * <p>{@code HOME} and {@code EXPERIENCE_LIST} always resolve — they point at
 * routes, not rows, and a route cannot be unpublished. {@code EXTERNAL_URL} is
 * off-site and not ours to verify beyond its scheme, which the write path and a
 * CHECK constraint already hold to http/https.
 */
public final class MenuTree {

    private MenuTree() {
    }

    /**
     * @param experienceHandles published experiences only; an id absent from it
     *                          is a link that goes
     * @param pageHandles       the same, for pages
     */
    public static List<MenuLinkData> build(List<MenuItemView> items,
                                           Map<UUID, String> experienceHandles,
                                           Map<UUID, String> pageHandles) {
        Map<UUID, List<MenuItemView>> byParent = new LinkedHashMap<>();
        for (MenuItemView item : items) {
            byParent.computeIfAbsent(item.parentId(), parent -> new ArrayList<>()).add(item);
        }
        return branch(byParent, null, experienceHandles, pageHandles);
    }

    private static List<MenuLinkData> branch(Map<UUID, List<MenuItemView>> byParent,
                                             UUID parentId,
                                             Map<UUID, String> experienceHandles,
                                             Map<UUID, String> pageHandles) {
        List<MenuLinkData> links = new ArrayList<>();
        for (MenuItemView item : byParent.getOrDefault(parentId, List.of())) {
            String targetHandle = switch (item.linkType()) {
                case "EXPERIENCE" -> experienceHandles.get(item.resourceId());
                case "PAGE" -> pageHandles.get(item.resourceId());
                default -> null;
            };
            if (needsATarget(item.linkType()) && targetHandle == null) {
                continue;
            }
            links.add(new MenuLinkData(
                    item.title(),
                    item.linkType(),
                    targetHandle,
                    item.url(),
                    branch(byParent, item.id(), experienceHandles, pageHandles)));
        }
        return List.copyOf(links);
    }

    private static boolean needsATarget(String linkType) {
        return linkType.equals("EXPERIENCE") || linkType.equals("PAGE");
    }
}
