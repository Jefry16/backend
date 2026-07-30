package com.vointika.touroperator.infrastructure.query;

import com.vointika.shared.port.NavigationItemView;
import com.vointika.shared.port.NavigationMenuView;
import com.vointika.shared.port.StorefrontNavigationQuery;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuItemJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.repository.MenuItemJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.MenuItemTranslationJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.MenuJpaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * touroperator's adapter for the storefront navigation seam.
 *
 * <p>Three queries regardless of how many menus or items the operator has: the
 * menus, all their items, and the localized titles for one locale. Navigation is
 * chrome — it renders on every page — so an N+1 here would be paid by every
 * request the storefront serves.
 *
 * <p>Items come back <em>unresolved</em>: this context knows an item points at
 * some experience, not that experiences live under {@code /experiences}. Turning
 * a target into a path belongs to whoever owns URL shape.
 */
@Component
public class StorefrontNavigationQueryImpl implements StorefrontNavigationQuery {

    private final MenuJpaRepository menuRepository;
    private final MenuItemJpaRepository menuItemRepository;
    private final MenuItemTranslationJpaRepository translationRepository;

    public StorefrontNavigationQueryImpl(MenuJpaRepository menuRepository,
                                         MenuItemJpaRepository menuItemRepository,
                                         MenuItemTranslationJpaRepository translationRepository) {
        this.menuRepository = menuRepository;
        this.menuItemRepository = menuItemRepository;
        this.translationRepository = translationRepository;
    }

    @Override
    public List<NavigationMenuView> findMenus(UUID tourOperatorId, String locale) {
        List<MenuJpaEntity> menus = menuRepository.findByTourOperatorIdOrderByHandleAsc(tourOperatorId);
        if (menus.isEmpty()) {
            return List.of();
        }

        List<UUID> menuIds = menus.stream().map(MenuJpaEntity::getId).toList();
        List<MenuItemJpaEntity> items = menuItemRepository.findByMenuIdInOrderByPositionAsc(menuIds);

        Map<UUID, String> titles = items.isEmpty()
                ? Map.of()
                : translationRepository
                        .findByMenuItemIdInAndLocale(items.stream().map(MenuItemJpaEntity::getId).toList(), locale)
                        .stream()
                        .filter(translation -> translation.getTitle() != null)
                        .collect(Collectors.toMap(
                                translation -> translation.getMenuItemId(),
                                translation -> translation.getTitle()));

        Map<UUID, List<MenuItemJpaEntity>> byMenu = items.stream()
                .collect(Collectors.groupingBy(MenuItemJpaEntity::getMenuId));

        return menus.stream()
                .map(menu -> new NavigationMenuView(
                        menu.getHandle(),
                        menu.getTitle(),
                        treeOf(byMenu.getOrDefault(menu.getId(), List.of()), titles)))
                .toList();
    }

    /**
     * Rebuilds the item tree from the flat rows.
     *
     * <p>Position order is preserved because the query returns them ordered and
     * the grouping below is insertion-ordered — the operator arranged this menu,
     * and the storefront must not quietly re-sort it.
     */
    private List<NavigationItemView> treeOf(List<MenuItemJpaEntity> items, Map<UUID, String> titles) {
        Map<UUID, List<MenuItemJpaEntity>> byParent = new LinkedHashMap<>();
        List<MenuItemJpaEntity> roots = new ArrayList<>();
        for (MenuItemJpaEntity item : items) {
            if (item.getParentId() == null) {
                roots.add(item);
            } else {
                byParent.computeIfAbsent(item.getParentId(), key -> new ArrayList<>()).add(item);
            }
        }
        return roots.stream().map(root -> toView(root, byParent, titles)).toList();
    }

    private NavigationItemView toView(MenuItemJpaEntity item,
                                      Map<UUID, List<MenuItemJpaEntity>> byParent,
                                      Map<UUID, String> titles) {
        return new NavigationItemView(
                // Translated title where the operator provided one, canonical otherwise.
                Objects.requireNonNullElse(titles.get(item.getId()), item.getTitle()),
                item.getLinkType(),
                item.getResourceId(),
                item.getUrl(),
                byParent.getOrDefault(item.getId(), List.of()).stream()
                        .map(child -> toView(child, byParent, titles))
                        .toList());
    }
}
