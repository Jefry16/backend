package com.vointika.rendering.presentation.response;

import com.vointika.rendering.application.dto.output.NavigationMenu;

import java.util.List;

/** One menu on the wire, addressed by its handle (`main-menu`, `footer`). */
public record NavigationMenuResponse(String handle, String title, List<NavigationItemResponse> items) {

    public static NavigationMenuResponse from(NavigationMenu menu) {
        return new NavigationMenuResponse(
                menu.handle(),
                menu.title(),
                menu.items().stream().map(NavigationItemResponse::from).toList());
    }
}
