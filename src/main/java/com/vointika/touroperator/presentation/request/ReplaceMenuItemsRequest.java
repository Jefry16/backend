package com.vointika.touroperator.presentation.request;

import com.vointika.touroperator.application.dto.input.ReplaceMenuItemsInput.MenuItemInput;

import java.util.List;

/**
 * The navigation editor's save: the menu's whole item tree, wholesale. Nesting
 * carries depth (max 3 levels), array order carries position.
 *
 * <p>The nodes bind straight to {@code MenuItemInput} (PATTERNS §4c): the
 * presentation copy of it was field-for-field identical, so it insulated
 * nothing while costing a recursive tree copy on every save. Only the wrapper
 * stays presentation-side — the input adds the caller and the two path ids.
 */
public record ReplaceMenuItemsRequest(List<MenuItemInput> items) {
}
