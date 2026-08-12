package com.vointika.storefront.application.dto.output;

import java.util.List;

/**
 * One of the operator's menus, resolved: every link here points somewhere that
 * exists, and the tree is the shape the operator arranged.
 *
 * @param handle what a theme reaches this menu by — {@code linklists["main-menu"]}
 * @param title  the operator's label for the menu. Not translated: it is what
 *               they call it in admin, not something a visitor reads.
 */
public record MenuData(String handle, String title, List<MenuLinkData> links) {

    /**
     * A link that survived resolution.
     *
     * <p><b>It carries a target, not a URL.</b> {@code targetHandle} is where the
     * thing lives; turning that into an address needs the locale prefix and the
     * route constants, which are presentation's. {@code externalUrl} is already
     * absolute and passes through — it is the operator's own link off the site.
     *
     * @param linkType {@code HOME}, {@code EXPERIENCE_LIST}, {@code EXPERIENCE},
     *                 {@code PAGE} or {@code EXTERNAL_URL}
     */
    public record MenuLinkData(String title,
                               String linkType,
                               String targetHandle,
                               String externalUrl,
                               List<MenuLinkData> links) {}
}
