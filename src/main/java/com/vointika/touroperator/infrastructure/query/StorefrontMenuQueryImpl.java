package com.vointika.touroperator.infrastructure.query;

import com.vointika.shared.port.StorefrontMenuQuery;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuItemJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuItemTranslationJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.repository.MenuItemJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.MenuItemTranslationJpaRepository;
import com.vointika.touroperator.infrastructure.persistence.repository.MenuJpaRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * touroperator's implementation of the storefront's navigation read.
 *
 * <p><b>Three queries for the whole navigation, whatever it contains</b>: the
 * menus, then every item across them, then every title translation for the
 * locale. An operator has two menus from creation and may have more; a query per
 * menu — or worse, per item — would be a page's worth of round trips for
 * something that renders in a header.
 *
 * <p>Item titles are the only translated part. A menu's own title is not: it is
 * the operator's label for the menu in admin, not something a visitor reads.
 */
@Component
public class StorefrontMenuQueryImpl implements StorefrontMenuQuery {

    private final MenuJpaRepository menuRepository;
    private final MenuItemJpaRepository itemRepository;
    private final MenuItemTranslationJpaRepository translationRepository;

    public StorefrontMenuQueryImpl(MenuJpaRepository menuRepository,
                                   MenuItemJpaRepository itemRepository,
                                   MenuItemTranslationJpaRepository translationRepository) {
        this.menuRepository = menuRepository;
        this.itemRepository = itemRepository;
        this.translationRepository = translationRepository;
    }

    @Override
    public List<MenuView> findMenus(UUID tourOperatorId, String locale) {
        List<MenuJpaEntity> menus = menuRepository.findByTourOperatorIdOrderByHandleAsc(tourOperatorId);
        if (menus.isEmpty()) {
            return List.of();
        }

        List<UUID> menuIds = menus.stream().map(MenuJpaEntity::getId).toList();
        List<MenuItemJpaEntity> items = itemRepository.findByMenuIdInOrderByMenuIdAscPositionAsc(menuIds);
        Map<UUID, String> translatedTitles = translatedTitles(items, locale);

        Map<UUID, List<MenuItemView>> byMenu = new LinkedHashMap<>();
        for (MenuItemJpaEntity item : items) {
            byMenu.computeIfAbsent(item.getMenuId(), id -> new ArrayList<>())
                    .add(new MenuItemView(
                            item.getId(),
                            item.getParentId(),
                            overlay(translatedTitles.get(item.getId()), item.getTitle()),
                            item.getLinkType(),
                            item.getResourceId(),
                            item.getUrl(),
                            item.getPosition()));
        }

        return menus.stream()
                .map(menu -> new MenuView(menu.getHandle(), menu.getTitle(),
                        List.copyOf(byMenu.getOrDefault(menu.getId(), List.of()))))
                .toList();
    }

    private Map<UUID, String> translatedTitles(List<MenuItemJpaEntity> items, String locale) {
        if (items.isEmpty()) {
            return Map.of();
        }
        List<UUID> itemIds = items.stream().map(MenuItemJpaEntity::getId).toList();
        Map<UUID, String> titles = new HashMap<>();
        for (MenuItemTranslationJpaEntity translation
                : translationRepository.findByMenuItemIdInAndLocale(itemIds, locale)) {
            titles.put(translation.getMenuItemId(), translation.getTitle());
        }
        return titles;
    }

    /** Nullable-wins-canonical, as everywhere else a translation overlays a row. */
    private static String overlay(String translated, String canonical) {
        return translated != null ? translated : canonical;
    }
}
