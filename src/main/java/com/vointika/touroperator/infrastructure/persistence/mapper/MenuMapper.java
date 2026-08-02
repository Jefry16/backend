package com.vointika.touroperator.infrastructure.persistence.mapper;

import com.vointika.shared.valueobject.LocaleCode;
import com.vointika.shared.valueobject.Handle;
import com.vointika.touroperator.domain.entity.Menu;
import com.vointika.touroperator.domain.entity.MenuItem;
import com.vointika.touroperator.domain.entity.MenuItemTranslation;
import com.vointika.touroperator.domain.enums.MenuItemLinkType;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuItemJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuItemTranslationJpaEntity;
import com.vointika.touroperator.infrastructure.persistence.entity.MenuJpaEntity;

public final class MenuMapper {

    private MenuMapper() {
    }

    public static MenuJpaEntity toJpa(Menu menu) {
        return new MenuJpaEntity(
                menu.getId(), menu.getTourOperatorId(), menu.getHandle().value(),
                menu.getTitle(), menu.getCreatedBy(), menu.getCreatedAt(), menu.getUpdatedAt());
    }

    public static Menu toDomain(MenuJpaEntity entity) {
        return new Menu(
                entity.getId(), entity.getTourOperatorId(), new Handle(entity.getHandle()),
                entity.getTitle(), entity.getCreatedBy(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static MenuItemJpaEntity toJpa(MenuItem item) {
        return new MenuItemJpaEntity(
                item.getId(), item.getMenuId(), item.getParentId(), item.getTitle(),
                item.getLinkType().name(), item.getResourceId(), item.getUrl(),
                item.getPosition(), item.getCreatedAt(), item.getUpdatedAt());
    }

    public static MenuItem toDomain(MenuItemJpaEntity entity) {
        return new MenuItem(
                entity.getId(), entity.getMenuId(), entity.getParentId(), entity.getTitle(),
                MenuItemLinkType.valueOf(entity.getLinkType()), entity.getResourceId(),
                entity.getUrl(), entity.getPosition(), entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static MenuItemTranslationJpaEntity toJpa(MenuItemTranslation translation) {
        return new MenuItemTranslationJpaEntity(
                translation.menuItemId(), translation.locale().value(), translation.title());
    }

    public static MenuItemTranslation toDomain(MenuItemTranslationJpaEntity entity) {
        return new MenuItemTranslation(
                entity.getMenuItemId(), new LocaleCode(entity.getLocale()), entity.getTitle());
    }
}
