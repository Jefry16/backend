package com.vointika.shared.port;

import java.util.List;

/** One of the operator's menus, its items already ordered and nested. */
public record NavigationMenuView(
        String handle,
        String title,
        List<NavigationItemView> items) {}
