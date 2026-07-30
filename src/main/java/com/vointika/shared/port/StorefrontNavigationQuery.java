package com.vointika.shared.port;

import java.util.List;
import java.util.UUID;

/**
 * Cross-context read of an operator's menus for the public storefront.
 * Implemented in {@code touroperator}, which owns them.
 *
 * <p>Item titles come back translated for the requested locale, falling back to
 * the operator's canonical title — the same per-field overlay every other
 * storefront read uses.
 */
public interface StorefrontNavigationQuery {

    /** Every menu the operator has, with its items ordered and nested. */
    List<NavigationMenuView> findMenus(UUID tourOperatorId, String locale);
}
