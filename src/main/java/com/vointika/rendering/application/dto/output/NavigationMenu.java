package com.vointika.rendering.application.dto.output;

import java.util.List;

/** One menu, ready to render. */
public record NavigationMenu(String handle, String title, List<NavigationItem> items) {}
