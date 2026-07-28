package com.vointika.touroperator.presentation.response;

import com.vointika.touroperator.domain.entity.Menu;

import java.time.Instant;
import java.util.UUID;

/** {@code id} + {@code context:"menus"} per the house rule. */
public record MenuListItemResponse(
        UUID id,
        String context,
        String handle,
        String title,
        Instant createdAt) {

    public static MenuListItemResponse from(Menu menu) {
        return new MenuListItemResponse(
                menu.getId(), "menus", menu.getHandle().value(),
                menu.getTitle(), menu.getCreatedAt());
    }
}
