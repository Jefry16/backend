package com.vointika.touroperator.application.dto.output;

import com.vointika.touroperator.domain.entity.Menu;

import java.util.List;

/** A menu with its full item tree, for the detail read. */
public record MenuDetail(Menu menu, List<MenuItemNode> items) {
}
