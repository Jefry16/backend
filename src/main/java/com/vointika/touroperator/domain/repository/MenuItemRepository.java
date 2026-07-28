package com.vointika.touroperator.domain.repository;

import com.vointika.touroperator.domain.entity.MenuItem;
import com.vointika.touroperator.domain.entity.MenuItemTranslation;

import java.util.List;
import java.util.UUID;

/**
 * Items are only ever written wholesale (the tree replace): delete the menu's
 * rows, insert the fresh set, insert the translations (the delete cascades to
 * the old translations at DB level). Reads return flat lists ordered by
 * {@code position}; the tree is assembled at the application layer.
 */
public interface MenuItemRepository {

    void deleteByMenuId(UUID menuId);

    void saveAll(List<MenuItem> items);

    void saveAllTranslations(List<MenuItemTranslation> translations);

    /** The menu's items, ordered by {@code position} ascending. */
    List<MenuItem> findByMenuId(UUID menuId);

    /** Every translation of the menu's items. */
    List<MenuItemTranslation> findTranslationsByMenuId(UUID menuId);
}
