package com.vointika.touroperator.application.dto.input;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ReplaceMenuItemsInput(
        UUID callerUserId,
        UUID tourOperatorId,
        UUID menuId,
        List<MenuItemInput> items
) {

    /** One submitted tree node; nesting carries depth, array order carries position. */
    public record MenuItemInput(
            String title,
            String linkType,
            UUID resourceId,
            String url,
            Map<String, String> titleTranslations,
            List<MenuItemInput> children
    ) {
    }
}
