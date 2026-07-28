package com.vointika.touroperator.presentation.request;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The navigation editor's save: the menu's whole item tree, wholesale. */
public record ReplaceMenuItemsRequest(List<MenuItemRequest> items) {

    /** Nesting carries depth (max 3 levels), array order carries position. */
    public record MenuItemRequest(
            String title,
            String linkType,
            UUID resourceId,
            String url,
            Map<String, String> titleTranslations,
            List<MenuItemRequest> children
    ) {
    }
}
