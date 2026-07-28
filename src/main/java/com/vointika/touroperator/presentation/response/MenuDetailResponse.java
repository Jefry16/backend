package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.application.dto.output.MenuDetail;
import com.vointika.touroperator.application.dto.output.MenuItemNode;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** {@code id} + {@code context:"menus"} per the house rule. */
public record MenuDetailResponse(
        UUID id,
        String context,
        String handle,
        String title,
        List<MenuItemResponse> items,
        Instant createdAt,
        Instant updatedAt) {

    /**
     * One item of the tree, children in position order. {@code resourceId}/
     * {@code url} are null unless the link type carries them;
     * {@code titleTranslations} is locale → title (empty when none).
     */
    public record MenuItemResponse(
            UUID id,
            String title,
            String linkType,
            UUID resourceId,
            String url,
            Map<String, String> titleTranslations,
            List<MenuItemResponse> children
    ) {

        static MenuItemResponse from(MenuItemNode node) {
            return new MenuItemResponse(
                    node.id(), node.title(), node.linkType().name(),
                    node.resourceId(), node.url(), node.titleTranslations(),
                    node.children().stream().map(MenuItemResponse::from).toList());
        }
    }

    public static MenuDetailResponse from(MenuDetail detail) {
        return new MenuDetailResponse(
                detail.menu().getId(),
                "menus",
                detail.menu().getHandle().value(),
                detail.menu().getTitle(),
                detail.items().stream().map(MenuItemResponse::from).toList(),
                detail.menu().getCreatedAt(),
                detail.menu().getUpdatedAt());
    }
}
