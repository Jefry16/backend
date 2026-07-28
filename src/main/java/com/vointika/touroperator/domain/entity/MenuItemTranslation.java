package com.vointika.touroperator.domain.entity;

import com.vointika.shared.exception.InvalidFieldException;
import com.vointika.shared.valueobject.LocaleCode;

import java.util.UUID;

/**
 * A per-locale override of a menu item's title, for the storefront render.
 * Not a standalone resource: items are rewritten wholesale on every save
 * (fresh ids each time), so translations ride inline in the items-replace
 * payload and are rewritten together with their items.
 */
public record MenuItemTranslation(UUID menuItemId, LocaleCode locale, String title) {

    private static final int TITLE_MAX_LENGTH = 120;

    public MenuItemTranslation {
        if (title == null || title.isBlank()) {
            throw new InvalidFieldException("Menu item translation title cannot be blank");
        }
        title = title.trim();
        if (title.length() > TITLE_MAX_LENGTH) {
            throw new InvalidFieldException(
                    "Menu item translation title must be at most " + TITLE_MAX_LENGTH + " characters");
        }
    }
}
