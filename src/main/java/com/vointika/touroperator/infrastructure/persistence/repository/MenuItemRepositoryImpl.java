package com.vointika.touroperator.infrastructure.persistence.repository;

import com.vointika.touroperator.domain.entity.MenuItem;
import com.vointika.touroperator.domain.entity.MenuItemTranslation;
import com.vointika.touroperator.domain.repository.MenuItemRepository;
import com.vointika.touroperator.infrastructure.persistence.mapper.MenuMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class MenuItemRepositoryImpl implements MenuItemRepository {

    private final MenuItemJpaRepository itemJpa;
    private final MenuItemTranslationJpaRepository translationJpa;

    public MenuItemRepositoryImpl(MenuItemJpaRepository itemJpa,
                                  MenuItemTranslationJpaRepository translationJpa) {
        this.itemJpa = itemJpa;
        this.translationJpa = translationJpa;
    }

    @Override
    public void deleteByMenuId(UUID menuId) {
        itemJpa.deleteByMenuId(menuId);
    }

    @Override
    public void saveAll(List<MenuItem> items) {
        itemJpa.saveAll(items.stream().map(MenuMapper::toJpa).toList());
    }

    @Override
    public void saveAllTranslations(List<MenuItemTranslation> translations) {
        translationJpa.saveAll(translations.stream().map(MenuMapper::toJpa).toList());
    }

    @Override
    public List<MenuItem> findByMenuId(UUID menuId) {
        return itemJpa.findByMenuIdOrderByPositionAsc(menuId).stream()
                .map(MenuMapper::toDomain)
                .toList();
    }

    @Override
    public List<MenuItemTranslation> findTranslationsByMenuId(UUID menuId) {
        return translationJpa.findByMenuId(menuId).stream()
                .map(MenuMapper::toDomain)
                .toList();
    }
}
