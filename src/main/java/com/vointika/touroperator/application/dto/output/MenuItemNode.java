package com.vointika.touroperator.application.dto.output;

import com.vointika.touroperator.domain.enums.MenuItemLinkType;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * One resolved node of a menu's item tree, children ordered by position
 * ascending. {@code titleTranslations} is locale → title (empty when the
 * item has none).
 */
public record MenuItemNode(
        UUID id,
        String title,
        MenuItemLinkType linkType,
        UUID resourceId,
        String url,
        Map<String, String> titleTranslations,
        List<MenuItemNode> children
) {
}
