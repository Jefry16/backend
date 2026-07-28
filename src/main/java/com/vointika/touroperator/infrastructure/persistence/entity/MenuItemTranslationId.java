package com.vointika.touroperator.infrastructure.persistence.entity;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite key of {@link MenuItemTranslationJpaEntity} (menu_item_id, locale). */
public class MenuItemTranslationId implements Serializable {

    private UUID menuItemId;
    private String locale;

    public MenuItemTranslationId() {
    }

    public MenuItemTranslationId(UUID menuItemId, String locale) {
        this.menuItemId = menuItemId;
        this.locale = locale;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MenuItemTranslationId other
                && Objects.equals(menuItemId, other.menuItemId)
                && Objects.equals(locale, other.locale);
    }

    @Override
    public int hashCode() {
        return Objects.hash(menuItemId, locale);
    }
}
