package com.vointika.rendering.presentation.response;

import com.vointika.rendering.application.dto.output.NavigationItem;

import java.util.List;

/** A menu item on the wire. The BFF turns {@code linkType} + {@code handle} into a path. */
public record NavigationItemResponse(
        String title,
        String linkType,
        String handle,
        String externalUrl,
        List<NavigationItemResponse> items) {

    public static NavigationItemResponse from(NavigationItem item) {
        return new NavigationItemResponse(
                item.title(),
                item.linkType(),
                item.handle(),
                item.externalUrl(),
                item.children().stream().map(NavigationItemResponse::from).toList());
    }
}
